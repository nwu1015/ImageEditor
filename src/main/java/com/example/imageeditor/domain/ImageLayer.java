package com.example.imageeditor.domain;

import com.example.imageeditor.service.CollageService;
import com.example.imageeditor.service.ImageService;
import com.example.imageeditor.service.Prototype;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

@Entity
@DiscriminatorValue("LEAF")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ImageLayer extends LayerComponent implements Prototype, Cloneable {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    private int width;
    private int height;
    private Integer cropX;
    private Integer cropY;
    private Integer cropWidth;
    private Integer cropHeight;

    @Column(columnDefinition = "TEXT")
    private String effectsJson;

    @Override
    public void render(Graphics2D g2d, ImageService imageService) throws IOException {
        BufferedImage img = imageService.applyTransformationsToLayer(this.getId());
        g2d.drawImage(img, getPositionX(), getPositionY(), null);
    }

    @Override
    public void applyUpdate(CollageService.LayerUpdateDTO dto) {
        Optional.ofNullable(dto.width).ifPresent(this::setWidth);
        Optional.ofNullable(dto.height).ifPresent(this::setHeight);
        Optional.ofNullable(dto.rotationAngle).ifPresent(this::setRotationAngle);
        Optional.ofNullable(dto.cropX).ifPresent(this::setCropX);
        Optional.ofNullable(dto.cropY).ifPresent(this::setCropY);
        Optional.ofNullable(dto.cropWidth).ifPresent(this::setCropWidth);
        Optional.ofNullable(dto.cropHeight).ifPresent(this::setCropHeight);
    }

    @Override
    public ImageLayer clone() {
        ImageLayer newLayer = (ImageLayer) super.clone();
        newLayer.setId(null);

        newLayer.setImage(this.getImage());
        newLayer.setWidth(this.width);
        newLayer.setHeight(this.height);
        newLayer.setCropX(this.cropX);
        newLayer.setCropY(this.cropY);
        newLayer.setCropWidth(this.cropWidth);
        newLayer.setCropHeight(this.cropHeight);
        newLayer.setEffectsJson(this.effectsJson);

        return newLayer;
    }

    @Override
    public ImageLayerMemento createMemento() {
        return new ImageLayerMemento(this);
    }

    public void restoreFromMemento(ImageLayerMemento memento) {
        this.setPositionX(memento.positionX());
        this.setPositionY(memento.positionY());
        this.setWidth(memento.width());
        this.setHeight(memento.height());
        this.setRotationAngle(memento.rotationAngle());
        this.setCropX(memento.cropX());
        this.setCropY(memento.cropY());
        this.setCropWidth(memento.cropWidth());
        this.setCropHeight(memento.cropHeight());
    }
}