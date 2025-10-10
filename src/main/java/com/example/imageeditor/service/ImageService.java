package com.example.imageeditor.service;

import com.example.imageeditor.domain.Collage;
import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.CollageRepository;
import com.example.imageeditor.repository.ImageLayerRepository;
import com.example.imageeditor.repository.ImageRepository;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final List<String> ALLOWED_FORMATS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp");

    private final Path rootLocation = Paths.get("uploads");

    private final ImageRepository imageRepository;

    private final ImageLayerRepository imageLayerRepository;

    private final CollageRepository collageRepository;

    public List<Image> findImagesByUser(User user) {
        // Просто викликаємо метод репозиторію
        return imageRepository.findByOwner(user);
    }

    public Image storeImage(MultipartFile file, User owner) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file.");
            }
            // ... ваша перевірка формату ...

            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
            Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();

            // === ВИЗНАЧЕННЯ РОЗМІРІВ ЗОБРАЖЕННЯ (ДОДАЙТЕ ЦЕЙ БЛОК) ===
            int width = 0;
            int height = 0;
            try (InputStream inputStream = file.getInputStream()) {
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                if (bufferedImage == null) {
                    throw new IOException("Could not read image file to determine dimensions.");
                }
                width = bufferedImage.getWidth();
                height = bufferedImage.getHeight();
            }
            // ==========================================================

            // Повторно використовуємо потік для збереження файлу
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            Image image = new Image();
            image.setFileName(uniqueFilename);
            image.setPath(destinationFile.toString());
            image.setFileFormat(fileExtension);
            image.setOwner(owner);

            // === ЗБЕРЕЖЕННЯ РОЗМІРІВ В БД ===
            image.setWidth(width);
            image.setHeight(height);
            // ===============================

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

        // ВАЖЛИВО: Перевірка, чи є поточний користувач власником зображення
        if (!image.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("You do not have permission to edit this image.");
        }

        image.setTitle(newTitle);
        return imageRepository.save(image);
    }

    @Transactional // Важливо додати цю анотацію
    public void deleteImage(Long imageId, User currentUser) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found!"));

        // Перевірка прав доступу
        if (!image.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("You do not have permission to delete this image.");
        }

        // === НОВА ЛОГІКА ОЧИЩЕННЯ КОЛАЖІВ ===
        // 1. Знаходимо всі шари, де використовується це зображення
        List<ImageLayer> layersToDelete = imageLayerRepository.findAllByImage(image);

        // 2. Збираємо унікальні колажі, до яких належали ці шари
        Set<Collage> collagesToDelete = layersToDelete.stream()
                .map(ImageLayer::getCollage)
                .collect(Collectors.toSet());

        // 3. Видаляємо ці колажі (разом з ними каскадно видаляться і всі їхні шари)
        if (!collagesToDelete.isEmpty()) {
            collageRepository.deleteAll(collagesToDelete);
        }
        // ===================================

        try {
            // 4. Тепер безпечно видаляємо файл з диска
            Path filePath = Paths.get(image.getPath());
            Files.deleteIfExists(filePath); // Використовуємо deleteIfExists для надійності

            // 5. І тільки тепер видаляємо сам запис про зображення
            imageRepository.delete(image);
        } catch (Exception e) {
            throw new RuntimeException("Could not delete the image.", e);
        }
    }



    public Resource getTransformedLayerAsResource(Long layerId) throws IOException {
        // 1. Отримуємо оброблене зображення з пам'яті
        BufferedImage transformedImage = applyTransformationsToLayer(layerId);

        // 2. Створюємо тимчасовий файл, щоб зберегти його на диск
        String tempFileName = "temp_" + UUID.randomUUID().toString() + ".png";
        Path tempFilePath = this.rootLocation.resolve(tempFileName);
        File tempFile = tempFilePath.toFile();

        // 3. Записуємо зображення у файл у форматі PNG (щоб підтримувати прозорість)
        ImageIO.write(transformedImage, "png", tempFile);

        // 4. Створюємо Resource, який контролер зможе віддати браузеру
        Resource resource = new UrlResource(tempFilePath.toUri());
        if (resource.exists() || resource.isReadable()) {
            // Важливо: після віддачі цей тимчасовий файл можна видаляти,
            // щоб не засмічувати диск. Для цього потрібна більш складна логіка.
            // Поки що залишаємо так для простоти.
            return resource;
        } else {
            throw new RuntimeException("Could not read the transformed file!");
        }
    }

    // === І ЦЕЙ ДОПОМІЖНИЙ МЕТОД ===
    public BufferedImage applyTransformationsToLayer(Long layerId) throws IOException {
        ImageLayer layer = imageLayerRepository.findById(layerId)
                .orElseThrow(() -> new RuntimeException("ImageLayer not found with ID: " + layerId));

        Image originalImage = layer.getImage();
        File originalFile = new File(originalImage.getPath());

        // Завантажуємо оригінальне зображення
        BufferedImage currentImage = ImageIO.read(originalFile);

        // Поворот
        if (layer.getRotationAngle() != 0.0) {
            double rotationRequired = Math.toRadians(layer.getRotationAngle());
            double locationX = currentImage.getWidth() / 2.0;
            double locationY = currentImage.getHeight() / 2.0;
            AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);

            // Розрахунок нового розміру полотна
            int newWidth = (int) (Math.abs(currentImage.getWidth() * Math.cos(rotationRequired)) + Math.abs(currentImage.getHeight() * Math.sin(rotationRequired)));
            int newHeight = (int) (Math.abs(currentImage.getWidth() * Math.sin(rotationRequired)) + Math.abs(currentImage.getHeight() * Math.cos(rotationRequired)));

            BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, currentImage.getType());
            Graphics2D g2d = rotatedImage.createGraphics();
            g2d.translate((newWidth - currentImage.getWidth()) / 2.0, (newHeight - currentImage.getHeight()) / 2.0);
            g2d.drawImage(currentImage, tx, null);
            g2d.dispose();
            currentImage = rotatedImage;
        }

        // Масштабування
        if (currentImage.getWidth() != layer.getWidth() || currentImage.getHeight() != layer.getHeight()) {
            BufferedImage scaledImage = new BufferedImage(layer.getWidth(), layer.getHeight(), currentImage.getType());
            Graphics2D g2d = scaledImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(currentImage, 0, 0, layer.getWidth(), layer.getHeight(), null);
            g2d.dispose();
            currentImage = scaledImage;
        }

        return currentImage;
    }

    public Image saveFinalImage(Image image) {
        return imageRepository.save(image);
    }
}
