package com.example.imageeditor.service;

import com.example.imageeditor.domain.Collage;
import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.CollageRepository;
import com.example.imageeditor.repository.ImageLayerRepository;
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
    public static class LayerUpdateDTO {
        public Integer width;
        public Integer height;
        public Double rotationAngle;
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

        Optional.ofNullable(dto.width).ifPresent(layer::setWidth);
        Optional.ofNullable(dto.height).ifPresent(layer::setHeight);
        Optional.ofNullable(dto.rotationAngle).ifPresent(layer::setRotationAngle);

        return imageLayerRepository.save(layer);
    }

    public Collage findCollageById(Long collageId) {
        return collageRepository.findById(collageId)
                .orElseThrow(() -> new RuntimeException("Collage not found with id: " + collageId));
    }

    @Transactional
    public Collage createCollage(String name, int canvasWidth, int canvasHeight, User user) {
        Collage newCollage = new Collage();

        newCollage.setName(name);
        newCollage.setCanvasWidth(canvasWidth);
        newCollage.setCanvasHeight(canvasHeight);
        newCollage.setUser(user); // Встановлюємо власника колажу

        // Зберігаємо новий колаж у базі даних і повертаємо його
        return collageRepository.save(newCollage);
    }

    @Transactional
    public ImageLayer addImageToCollage(Long collageId, MultipartFile file, User user) throws IOException {
        // 1. Спочатку зберігаємо завантажений файл і створюємо сутність Image
        Image image = imageService.storeImage(file, user);

        // 2. Знаходимо колаж, до якого додаємо зображення
        Collage collage = findCollageById(collageId);

        // 3. Створюємо новий шар (ImageLayer)
        ImageLayer newLayer = new ImageLayer();
        newLayer.setImage(image);
        newLayer.setCollage(collage);
        newLayer.setWidth(image.getWidth());
        newLayer.setHeight(image.getHeight());
        newLayer.setPositionX(0); // Початкова позиція
        newLayer.setPositionY(0);

        // Встановлюємо zIndex, щоб шар з'явився поверх інших
        int maxZIndex = collage.getLayers().stream()
                .mapToInt(ImageLayer::getZIndex)
                .max().orElse(-1);
        newLayer.setZIndex(maxZIndex + 1);

        return imageLayerRepository.save(newLayer);
    }

    // === І ЦЕЙ МЕТОД ===
    @Transactional
    public ImageLayer updateLayerAction(Long layerId, String action) {
        ImageLayer layer = imageLayerRepository.findById(layerId)
                .orElseThrow(() -> new RuntimeException("Layer not found with id: " + layerId));

        switch (action) {
            case "rotate_right":
                layer.setRotationAngle(layer.getRotationAngle() + 90);
                break;
            case "rotate_left":
                layer.setRotationAngle(layer.getRotationAngle() - 90);
                break;
            case "delete":
                imageLayerRepository.delete(layer);
                return null; // Повертаємо null, оскільки шар видалено
            // Можна додати інші дії, наприклад "bring_forward", "send_backward"
            // case "bring_forward":
            //     layer.setZIndex(layer.getZIndex() + 1);
            //     break;
        }

        return imageLayerRepository.save(layer);
    }

    // === ДОДАЙТЕ ЦЕЙ МЕТОД ===
    @Transactional
    public Image renderAndSaveCollage(Long collageId, User user) throws IOException {
        // 1. Знаходимо колаж з усіма його шарами
        Collage collage = findCollageById(collageId);

        // 2. Створюємо пусте "полотно" з розмірами колажу
        BufferedImage canvas = new BufferedImage(
                collage.getCanvasWidth(),
                collage.getCanvasHeight(),
                BufferedImage.TYPE_INT_ARGB // Тип, що підтримує прозорість
        );
        Graphics2D g2d = canvas.createGraphics();

        // 3. Проходимо по всіх шарах (вони вже відсортовані по zIndex) і малюємо їх
        for (ImageLayer layer : collage.getLayers()) {
            // Отримуємо трансформоване зображення для кожного шару
            BufferedImage transformedLayerImage = imageService.applyTransformationsToLayer(layer.getId());

            // Малюємо шар на полотні у вказаній позиції
            g2d.drawImage(transformedLayerImage, layer.getPositionX(), layer.getPositionY(), null);
        }
        g2d.dispose(); // Звільняємо ресурси

        // 4. Зберігаємо фінальне зображення у файл
        String finalFileName = "collage-" + UUID.randomUUID().toString() + ".png";
        Path finalPath = Paths.get("uploads").resolve(finalFileName);
        ImageIO.write(canvas, "png", finalPath.toFile());

        // 5. (Опціонально, але рекомендовано) Створюємо новий запис Image для фінального колажу
        Image finalImage = new Image();
        finalImage.setFileName(finalFileName);
        finalImage.setPath(finalPath.toString());
        finalImage.setFileFormat("png");
        finalImage.setWidth(canvas.getWidth());
        finalImage.setHeight(canvas.getHeight());
        finalImage.setOwner(user);
        finalImage.setTitle("Результат колажу: " + collage.getName());

        // Зберігаємо інформацію про фінальний файл у БД
        return imageService.saveFinalImage(finalImage); // Вам треба буде створити цей простий метод в ImageService
    }
}