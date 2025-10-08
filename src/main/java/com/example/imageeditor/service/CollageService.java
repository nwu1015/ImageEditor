package com.example.imageeditor.service;

import com.example.imageeditor.domain.*;
import com.example.imageeditor.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class CollageService {

    @Autowired private CollageRepository collageRepository;
    @Autowired private ImageRepository imageRepository;
    @Autowired private ImageLayerRepository imageLayerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FileStorageService fileStorageService; // З попереднього прикладу

    public Collage findCollageById(Long id) {
        return collageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collage with id " + id + " not found"));
    }

    @Transactional
    public void addImageToCollage(Long collageId, MultipartFile file) throws IOException {
        Collage collage = findCollageById(collageId);
        User user = collage.getUser(); // Припускаємо, що колаж вже має юзера

        // 1. Зберігаємо файл
        String filePath = fileStorageService.storeFile(file);
        BufferedImage bimg = ImageIO.read(new File(filePath));

        // 2. Створюємо сутність Image
        Image image = new Image();
        image.setOwner(user);
        image.setPath(filePath);
        image.setFileName(file.getOriginalFilename());
        image.setFileFormat(file.getContentType());
        image.setWidth(bimg.getWidth());
        image.setHeight(bimg.getHeight());
        imageRepository.save(image);

        // 3. Створюємо сутність ImageLayer
        ImageLayer newLayer = new ImageLayer();
        newLayer.setCollage(collage); // Використовуйте setProject, якщо поле так називається
        newLayer.setImage(image);
        newLayer.setPositionX(20); // Початкові координати
        newLayer.setPositionY(20);
        newLayer.setWidth(image.getWidth() / 4); // Зменшимо для зручності
        newLayer.setHeight(image.getHeight() / 4);
        newLayer.setZIndex(collage.getLayers().size());
        imageLayerRepository.save(newLayer);
    }

    @Transactional
    public void updateLayerAction(Long layerId, String action) {
        ImageLayer layer = imageLayerRepository.findById(layerId)
                .orElseThrow(() -> new RuntimeException("Layer not found"));

        switch (action) {
            case "rotate":
                double newAngle = layer.getRotationAngle() + 45.0;
                layer.setRotationAngle(newAngle);
                break;
            case "delete":
                imageLayerRepository.delete(layer);
                break;
            case "move_right":
                layer.setPositionX(layer.getPositionX() + 20);
                break;
            case "move_left":
                layer.setPositionX(layer.getPositionX() - 20);
                break;
            // Додайте інші дії за потреби
        }

        // Зберігаємо зміни (якщо шар не був видалений)
        if (!action.equals("delete")) {
            imageLayerRepository.save(layer);
        }
    }

    public List<Collage> findAllCollages() {
        return collageRepository.findAll();
    }

    @Transactional
    public Collage createCollage(String name, int width, int height, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Collage collage = new Collage();
        collage.setName(name);
        collage.setCanvasWidth(width);
        collage.setCanvasHeight(height);
        collage.setUser(user);

        return collageRepository.save(collage);
    }
}
