package com.example.imageeditor.service;

import com.example.imageeditor.domain.Collage;
import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.CollageRepository;
import com.example.imageeditor.repository.ImageRepository;
import com.example.imageeditor.repository.LayerComponentRepository;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import java.awt.RenderingHints;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final Path rootLocation = Paths.get("uploads");

    private final ImageRepository imageRepository;

    private final LayerComponentRepository layerComponentRepository;

    private final CollageRepository collageRepository;

    public List<Image> findImagesByUser(User user) {
        return imageRepository.findByOwner(user);
    }

    public Image storeImage(MultipartFile file, User owner) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file.");
            }

            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID() + "." + fileExtension;
            Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            int width, height;
            try {
                BufferedImage bufferedImage = ImageIO.read(destinationFile.toFile());

                if (bufferedImage == null) {
                    Files.delete(destinationFile);
                    throw new IOException("Файл збережено, але він не є валідним зображенням (jpg, png, gif).");
                }
                width = bufferedImage.getWidth();
                height = bufferedImage.getHeight();
            } catch (IOException e) {
                try { Files.deleteIfExists(destinationFile); } catch (IOException ignored) {}
                throw new RuntimeException("Не вдалося прочитати розміри збереженого файлу.", e);
            }

            Image image = new Image();
            image.setFileName(uniqueFilename);
            image.setPath(destinationFile.toString());
            image.setFileFormat(fileExtension);
            image.setOwner(owner);

            image.setWidth(width);
            image.setHeight(height);

            return imageRepository.save(image);

        } catch (Exception e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public Image updateImageTitle(Long imageId, String newTitle, User currentUser) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found!"));

        if (!image.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("You do not have permission to edit this image.");
        }

        image.setTitle(newTitle);
        return imageRepository.save(image);
    }

    @Transactional
    public void deleteImage(Long imageId, User currentUser) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found!"));

        if (!image.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("You do not have permission to delete this image.");
        }

        List<ImageLayer> layersToDelete = layerComponentRepository.findAllImageLayersByImage(image);

        Set<Collage> collagesToDelete = layersToDelete.stream()
                .map(ImageLayer::getCollage)
                .collect(Collectors.toSet());

        if (!collagesToDelete.isEmpty()) {
            collageRepository.deleteAll(collagesToDelete);
        }

        try {
            Path filePath = Paths.get(image.getPath());
            Files.deleteIfExists(filePath);

            imageRepository.delete(image);
        } catch (Exception e) {
            throw new RuntimeException("Could not delete the image.", e);
        }
    }

    public Resource getTransformedLayerAsResource(Long layerId) throws IOException {
        BufferedImage transformedImage = applyTransformationsToLayer(layerId);

        String tempFileName = "temp_" + UUID.randomUUID() + ".png";
        Path tempFilePath = this.rootLocation.resolve(tempFileName);
        File tempFile = tempFilePath.toFile();

        ImageIO.write(transformedImage, "png", tempFile);

        Resource resource = new UrlResource(tempFilePath.toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Could not read the transformed file!");
        }
    }

    public BufferedImage applyTransformationsToLayer(Long layerId) throws IOException {
        ImageLayer layer = layerComponentRepository.findImageLayerById(layerId)
                .orElseThrow(() -> new RuntimeException("ImageLayer (Leaf) not found with ID: " + layerId));

        Image originalImage = layer.getImage();
        File originalFile = new File(originalImage.getPath());

        BufferedImage currentImage = ImageIO.read(originalFile);

        if (layer.getCropX() != null && layer.getCropY() != null &&
                layer.getCropWidth() != null && layer.getCropHeight() != null &&
                layer.getCropWidth() > 0 && layer.getCropHeight() > 0) {

            currentImage = currentImage.getSubimage(
                    layer.getCropX(),
                    layer.getCropY(),
                    layer.getCropWidth(),
                    layer.getCropHeight()
            );
        }

        BufferedImage scaledImage = new BufferedImage(layer.getWidth(),
                layer.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(currentImage, 0, 0, layer.getWidth(), layer.getHeight(), null);
        g2d.dispose();
        currentImage = scaledImage;

        if (layer.getRotationAngle() != 0.0) {
            double rads = Math.toRadians(layer.getRotationAngle());
            double sin = Math.abs(Math.sin(rads));
            double cos = Math.abs(Math.cos(rads));
            int w = currentImage.getWidth();
            int h = currentImage.getHeight();
            int newWidth = (int) Math.floor(w * cos + h * sin);
            int newHeight = (int) Math.floor(h * cos + w * sin);

            BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = rotatedImage.createGraphics();
            g2.translate((newWidth - w) / 2.0, (newHeight - h) / 2.0);
            g2.rotate(rads, w / 2.0, h / 2.0);
            g2.drawRenderedImage(currentImage, null);
            g2.dispose();
            currentImage = rotatedImage;
        }

        return currentImage;
    }

    @Transactional
    public Image saveRenderedCollage(BufferedImage canvas, Collage collage, User user){
        String fileExtension = "png";
        String uniqueFilename = "collage-" + UUID.randomUUID() + "." + fileExtension;
        Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();

        try {
            ImageIO.write(canvas, fileExtension, destinationFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save rendered collage", e);
        }

        Image finalImage = new Image();
        finalImage.setFileName(uniqueFilename);
        finalImage.setPath(destinationFile.toString());
        finalImage.setFileFormat(fileExtension);
        finalImage.setOwner(user);
        finalImage.setWidth(canvas.getWidth());
        finalImage.setHeight(canvas.getHeight());
        finalImage.setTitle("Результат колажу: " + collage.getName());

        return imageRepository.save(finalImage);
    }
}