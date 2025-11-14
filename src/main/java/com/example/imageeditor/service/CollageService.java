package com.example.imageeditor.service;

import com.example.imageeditor.domain.*;
import com.example.imageeditor.domain.Image;
import com.example.imageeditor.repository.CollageRepository;
import com.example.imageeditor.repository.LayerComponentRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CollageService {

    private final CollageRepository collageRepository;
    private final LayerComponentRepository layerComponentRepository;
    private final ImageService imageService;

    private final Map<Long, Stack<ImageLayerMemento>> undoStacks = new ConcurrentHashMap<>();
    private final Map<Long, Stack<ImageLayerMemento>> redoStacks = new ConcurrentHashMap<>();

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

    private void saveUndoState(Long collageId, ImageLayer layer) {
        Stack<ImageLayerMemento> undoStack = undoStacks.computeIfAbsent(collageId, k -> new Stack<>());
        Stack<ImageLayerMemento> redoStack = redoStacks.computeIfAbsent(collageId, k -> new Stack<>());

        undoStack.push(layer.createMemento());

        redoStack.clear();
    }

    @Transactional
    public ImageLayer findOrCreateLayerForImage(Image image, User user) {
        return layerComponentRepository.findFirstImageLayerByImageAndUser(image, user)
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
                    return layerComponentRepository.save(layer);
                });
    }

    @Transactional
    public ImageLayer updateImageLayer(Long layerId, LayerUpdateDTO dto) {
        ImageLayer layer = layerComponentRepository.findImageLayerById(layerId)
                .orElseThrow(() -> new RuntimeException("ImageLayer (Leaf) not found with ID: " + layerId));

        Collage collage = layer.getCollage();
        collage.getCurrentState().checkCanEdit(collage);

        saveUndoState(layer.getCollage().getId(), layer);

        Optional.ofNullable(dto.width).ifPresent(layer::setWidth);
        Optional.ofNullable(dto.height).ifPresent(layer::setHeight);
        Optional.ofNullable(dto.rotationAngle).ifPresent(layer::setRotationAngle);
        Optional.ofNullable(dto.cropX).ifPresent(layer::setCropX);
        Optional.ofNullable(dto.cropY).ifPresent(layer::setCropY);
        Optional.ofNullable(dto.cropWidth).ifPresent(layer::setCropWidth);
        Optional.ofNullable(dto.cropHeight).ifPresent(layer::setCropHeight);

        return layerComponentRepository.save(layer);
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
                .mapToInt(LayerComponent::getZIndex)
                .max().orElse(-1);
        newLayer.setZIndex(maxZIndex + 1);

        return layerComponentRepository.save(newLayer);
    }

    @Transactional
    public LayerComponent updateLayerAction(Long layerId, String action) {
        LayerComponent component = layerComponentRepository.findById(layerId)
                .orElseThrow(() -> new RuntimeException("LayerComponent not found with id: " + layerId));

        Collage collage = component.getCollage();
        collage.getCurrentState().checkCanEdit(collage);

        if (component instanceof ImageLayer layer) {
            saveUndoState(layer.getCollage().getId(), layer);
        }

        switch (action) {
            case "rotate_right":
                component.setRotationAngle(component.getRotationAngle() + 90);
                break;
            case "rotate_left":
                component.setRotationAngle(component.getRotationAngle() - 90);
                break;
            case "delete":
                layerComponentRepository.delete(component);
                return null;
        }
        return layerComponentRepository.save(component);
    }

    @Transactional
    public Image renderAndSaveCollage(Long collageId, User user) throws IOException {
        Collage collage = findCollageById(collageId);

        BufferedImage canvas = new BufferedImage(
                collage.getCanvasWidth(),
                collage.getCanvasHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = canvas.createGraphics();

        for (LayerComponent component : collage.getLayers()) {
            component.render(g2d, imageService);
        }

        g2d.dispose();
        return imageService.saveRenderedCollage(canvas, collage, user);
    }

    @Transactional
    public void duplicateLayer(Long layerId) {
        LayerComponent prototypeComponent = layerComponentRepository.findById(layerId)
                .orElseThrow(() -> new RuntimeException("Layer component not found: " + layerId));

        Collage collage = prototypeComponent.getCollage();
        collage.getCurrentState().checkCanEdit(collage);

        LayerComponent newComponent = prototypeComponent.clone();

        int maxZIndex = collage.getLayers().stream()
                .mapToInt(LayerComponent::getZIndex)
                .max().orElse(-1);
        newComponent.setZIndex(maxZIndex + 1);
        newComponent.setCollage(collage);

        layerComponentRepository.save(newComponent);
    }

    @Transactional
    public ImageLayer undo(Long collageId) {
        Stack<ImageLayerMemento> undoStack = undoStacks.get(collageId);
        Stack<ImageLayerMemento> redoStack = redoStacks.get(collageId);

        if (undoStack == null || undoStack.isEmpty()) {
            return null;
        }

        ImageLayerMemento mementoToRestore = undoStack.pop();

        ImageLayer layer = layerComponentRepository.findImageLayerById(mementoToRestore.layerId())
                .orElseThrow(() -> new RuntimeException("Layer not found"));

        if (redoStack == null) redoStack = new Stack<>();
        redoStack.push(layer.createMemento());
        redoStacks.put(collageId, redoStack);

        layer.restoreFromMemento(mementoToRestore);

        return layerComponentRepository.save(layer);
    }

    @Transactional
    public ImageLayer redo(Long collageId) {
        Stack<ImageLayerMemento> undoStack = undoStacks.get(collageId);
        Stack<ImageLayerMemento> redoStack = redoStacks.get(collageId);

        if (redoStack == null || redoStack.isEmpty()) {
            return null;
        }

        ImageLayerMemento mementoToRestore = redoStack.pop();

        ImageLayer layer = layerComponentRepository.findImageLayerById(mementoToRestore.layerId())
                .orElseThrow(() -> new RuntimeException("Layer not found"));

        if (undoStack == null) undoStack = new Stack<>();
        undoStack.push(layer.createMemento());
        undoStacks.put(collageId, undoStack);

        layer.restoreFromMemento(mementoToRestore);

        return layerComponentRepository.save(layer);
    }

}