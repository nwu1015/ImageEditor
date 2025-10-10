package com.example.imageeditor.service;

import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final List<String> ALLOWED_FORMATS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp");

    private final Path rootLocation = Paths.get("uploads");

    private final ImageRepository imageRepository;

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

            // --- ПЕРЕВІРКА ФОРМАТУ ---
            String fileExtension = getFileExtension(originalFilename);
            if (!ALLOWED_FORMATS.contains(fileExtension.toLowerCase())) {
                throw new RuntimeException("Invalid file format. Allowed formats are: " + ALLOWED_FORMATS);
            }

            // Створюємо унікальне ім'я файлу, щоб уникнути конфліктів
            String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;

            // Зберігаємо файл у файлову систему
            Path destinationFile = this.rootLocation.resolve(uniqueFilename).normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // Зберігаємо інформацію в БД
            Image image = new Image();
            image.setFileName(uniqueFilename);
            image.setPath(destinationFile.toString());
            image.setFileFormat(fileExtension);
            image.setOwner(owner);
            // Тут можна додати логіку для визначення висоти/ширини

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

    public void deleteImage(Long imageId, User currentUser) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found!"));

        // ВАЖЛИВО: Перевірка прав доступу
        if (!image.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("You do not have permission to delete this image.");
        }

        try {
            // 1. Видаляємо файл з диска
            Path filePath = Paths.get(image.getPath());
            Files.delete(filePath);

            // 2. Видаляємо запис з БД
            imageRepository.delete(image);
        } catch (Exception e) {
            throw new RuntimeException("Could not delete the image.", e);
        }
    }
}
