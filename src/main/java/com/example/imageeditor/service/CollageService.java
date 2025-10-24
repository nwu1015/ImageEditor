package com.example.imageeditor.service;

import com.example.imageeditor.domain.Collage;
import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.CollageRepository;
import com.example.imageeditor.repository.ImageLayerRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollageService {

    private final CollageRepository collageRepository;
    private final ImageLayerRepository imageLayerRepository;
    private final ImageService imageService;

    // DTO для оновлення шару
    @Data
    public static class LayerUpdateDTO {
        // Для розтягування/стиснення
        public Integer width;
        public Integer height;

        // Для повороту
        public Double rotationAngle;

        // Для кадрування
        public Integer cropX;
        public Integer cropY;
        public Integer cropWidth;
        public Integer cropHeight;
    }

    @Transactional
    public ImageLayer findOrCreateLayerForImage(Image image, User user) {
        return imageLayerRepository.findFirstByImageAndCollage_User(image, user)
                .orElseGet(() -> {
                    Collage collage = new Collage();
                    collage.setName("Collage_for_image_" + image.getId());
                    collage.setUser(user);
                    collage.setCanvasWidth(image.getWidth());
                    collage.setCanvasHeight(image.getHeight());
                    Collage savedCollage = collageRepository.save(collage);

                    ImageLayer layer = new ImageLayer();
                    layer.setImage(image);
                    layer.setCollage(savedCollage);
                    layer.setWidth(image.getWidth());
                    layer.setHeight(image.getHeight());
                    return imageLayerRepository.save(layer);
                });
    }

    @Transactional
    public ImageLayer updateImageLayer(Long layerId, LayerUpdateDTO dto) {
        ImageLayer layer = imageLayerRepository.findById(layerId)
                .orElseThrow(() -> new RuntimeException("ImageLayer not found with ID: " + layerId));

        Collage collage = layer.getCollage();
        collage.getCurrentState().checkCanEdit(collage);

        Optional.ofNullable(dto.width).ifPresent(layer::setWidth);
        Optional.ofNullable(dto.height).ifPresent(layer::setHeight);
        Optional.ofNullable(dto.rotationAngle).ifPresent(layer::setRotationAngle);
        Optional.ofNullable(dto.cropX).ifPresent(layer::setCropX);
        Optional.ofNullable(dto.cropY).ifPresent(layer::setCropY);
        Optional.ofNullable(dto.cropWidth).ifPresent(layer::setCropWidth);
        Optional.ofNullable(dto.cropHeight).ifPresent(layer::setCropHeight);

        return imageLayerRepository.save(layer);
    }

    public Collage findCollageById(Long collageId) {
        return collageRepository.findById(collageId)
                .orElseThrow(() -> new RuntimeException("Collage not found with id: " + collageId));
    }

    @Transactional
    public ImageLayer addImageToCollage(Long collageId, MultipartFile file, User user) throws IOException {
        Collage collage = findCollageById(collageId);
        collage.getCurrentState().checkCanEdit(collage);

        Image image = imageService.storeImage(file, user);

        ImageLayer newLayer = new ImageLayer();
        newLayer.setImage(image);
        newLayer.setCollage(collage);
        newLayer.setWidth(image.getWidth());
        newLayer.setHeight(image.getHeight());
        newLayer.setPositionX(0);
        newLayer.setPositionY(0);

        int maxZIndex = collage.getLayers().stream()
                .mapToInt(ImageLayer::getZIndex)
                .max().orElse(-1);
        newLayer.setZIndex(maxZIndex + 1);

        return imageLayerRepository.save(newLayer);
    }

    @Transactional
    public ImageLayer updateLayerAction(Long layerId, String action) {
        ImageLayer layer = imageLayerRepository.findById(layerId)
                .orElseThrow(() -> new RuntimeException("Layer not found with id: " + layerId));

        Collage collage = layer.getCollage();
        collage.getCurrentState().checkCanEdit(collage);

        switch (action) {
            case "rotate_right":
                layer.setRotationAngle(layer.getRotationAngle() + 90);
                break;
            case "rotate_left":
                layer.setRotationAngle(layer.getRotationAngle() - 90);
                break;
            case "delete":
                imageLayerRepository.delete(layer);
                return null;
        }
        return imageLayerRepository.save(layer);
    }

    @Transactional
    public Image renderAndSaveCollage(Long collageId, User user) throws IOException {
        Collage collage = findCollageById(collageId);

        BufferedImage canvas = new BufferedImage(
                collage.getCanvasWidth(),
                collage.getCanvasHeight(),
                BufferedImage.TYPE_INT_ARGB // Тип, що підтримує прозорість
        );
        Graphics2D g2d = canvas.createGraphics();

        for (ImageLayer layer : collage.getLayers()) {
            BufferedImage transformedLayerImage =
                    imageService.applyTransformationsToLayer(layer.getId());

            g2d.drawImage(transformedLayerImage, layer.getPositionX(),
                    layer.getPositionY(), null);
        }
        g2d.dispose();

        String finalFileName = "collage-" + UUID.randomUUID() + ".png";
        Path finalPath = Paths.get("uploads").resolve(finalFileName);
        ImageIO.write(canvas, "png", finalPath.toFile());

        Image finalImage = new Image();
        finalImage.setFileName(finalFileName);
        finalImage.setPath(finalPath.toString());
        finalImage.setFileFormat("png");
        finalImage.setWidth(canvas.getWidth());
        finalImage.setHeight(canvas.getHeight());
        finalImage.setOwner(user);
        finalImage.setTitle("Результат колажу: " + collage.getName());

        return imageService.saveFinalImage(finalImage);
    }

    @Transactional
    public void duplicateLayer(Long layerId) {
        Prototype<ImageLayer> prototypeLayer = imageLayerRepository.findById(layerId)
                .orElseThrow(() -> new RuntimeException("Layer not found: " + layerId));

        Collage collage = ((ImageLayer) prototypeLayer).getCollage();
        collage.getCurrentState().checkCanEdit(collage);

        ImageLayer newLayer = prototypeLayer.clone();

        int maxZIndex = collage.getLayers().stream()
                .mapToInt(ImageLayer::getZIndex)
                .max().orElse(-1);
        newLayer.setZIndex(maxZIndex + 1);

        imageLayerRepository.save(newLayer);
    }

}