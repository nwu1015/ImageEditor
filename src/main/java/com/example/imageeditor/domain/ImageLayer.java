package com.example.imageeditor.domain;

import com.example.imageeditor.service.Prototype;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList; // Додано
import java.util.List; // Додано

@Entity
@Table(name = "image_layers")
@Data
@NoArgsConstructor
public class ImageLayer implements Prototype, Cloneable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Collage collage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    private int positionX;
    private int positionY;
    private double rotationAngle = 0.0;
    private int zIndex;

    private int width;
    private int height;
    private Integer cropX;
    private Integer cropY;
    private Integer cropWidth;
    private Integer cropHeight;
    @Column(columnDefinition = "TEXT")
    private String effectsJson;


    @Column(name = "is_group", nullable = false, columnDefinition = "boolean default false")
    private boolean isGroup = false; // За замовчуванням це "Листок"

    // Список "нащадків", якщо isGroup = true.
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_layer_id")
    @OrderBy("zIndex ASC")
    private List<ImageLayer> children = new ArrayList<>();

    public void add(ImageLayer child) {
        if (!this.isGroup) {
            throw new UnsupportedOperationException("Unable to add a child to 'Leaf'.");
        }
        children.add(child);
    }

    public void remove(ImageLayer child) {
        if (!this.isGroup) {
            throw new UnsupportedOperationException("Unable to delete a child to 'Leaf'.");
        }
        children.remove(child);
    }

    @Override
    public ImageLayer clone() {
        try {
            ImageLayer newLayer = (ImageLayer) super.clone();

            newLayer.setId(null);
            newLayer.setCollage(this.getCollage());
            newLayer.setGroup(this.isGroup);

            newLayer.setChildren(new ArrayList<>());

            newLayer.setPositionX(this.positionX);
            newLayer.setPositionY(this.positionY);
            newLayer.setRotationAngle(this.rotationAngle);

            if (this.isGroup) {
                for (ImageLayer child : this.children) {
                    ImageLayer clonedChild = child.clone(); // Рекурсивний виклик
                    newLayer.add(clonedChild);
                }
            } else {
                // Якщо це "Листок", копіюємо його унікальні властивості
                newLayer.setImage(this.getImage());
                newLayer.setWidth(this.width);
                newLayer.setHeight(this.height);
                newLayer.setCropX(this.cropX);
                newLayer.setCropY(this.cropY);
                newLayer.setCropWidth(this.cropWidth);
                newLayer.setCropHeight(this.cropHeight);
                newLayer.setEffectsJson(this.effectsJson);
            }

            return newLayer;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Can't clone a layer.", e);
        }
    }

    public ImageLayerMemento createMemento() {
        if (this.isGroup) {
            throw new UnsupportedOperationException("Memento not supported for this group.");
        }
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