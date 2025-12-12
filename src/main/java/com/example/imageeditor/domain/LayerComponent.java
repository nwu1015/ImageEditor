package com.example.imageeditor.domain;

import com.example.imageeditor.service.CollageService;
import com.example.imageeditor.service.ImageService;
import com.example.imageeditor.service.Prototype;
import jakarta.persistence.*;
import lombok.Data;

import java.awt.Graphics2D;
import java.io.IOException;

@Entity
@Table(name = "layer_components")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "component_type")
@Data
public abstract class LayerComponent implements Prototype, Cloneable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int positionX;
    private int positionY;
    private double rotationAngle = 0.0;
    private int zIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collage_id")
    private Collage collage;

    public abstract void render(Graphics2D g2d, ImageService imageService) throws IOException;

    public abstract void applyUpdate(CollageService.LayerUpdateDTO dto);

    public abstract ImageLayerMemento createMemento();

    public abstract void restoreFromMemento(ImageLayerMemento memento);

    @Override
    public LayerComponent clone() {
        try {
            return (LayerComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Неможливо клонувати компонент", e);
        }
    }
}