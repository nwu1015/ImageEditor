package com.example.imageeditor.domain;

import com.example.imageeditor.service.Prototype;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "image_layers")
@Data
@NoArgsConstructor
public class ImageLayer implements Prototype<ImageLayer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Collage collage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    //Властивості трансформації
    private int positionX;
    private int positionY;

    private int width;  // Ширина шару
    private int height; // Висота шару

    private double rotationAngle = 0.0; // Кут повороту в градусах

    private int zIndex; // Порядок шару (0 - найнижчий, вищі значення - ближче до глядача)

    // Параметри кадрування
    private Integer cropX;
    private Integer cropY;
    private Integer cropWidth;
    private Integer cropHeight;

    @Column(columnDefinition = "TEXT")
    private String effectsJson;



    @Override
    public ImageLayer clone() {
        try {
            ImageLayer newLayer = (ImageLayer) super.clone();

            newLayer.setCollage(this.getCollage());
            newLayer.setImage(this.getImage());

            newLayer.setWidth(this.width);
            newLayer.setHeight(this.height);
            newLayer.setRotationAngle(this.rotationAngle);
            newLayer.setCropX(this.cropX);
            newLayer.setCropY(this.cropY);
            newLayer.setCropWidth(this.cropWidth);
            newLayer.setCropHeight(this.cropHeight);
            newLayer.setPositionX(this.positionX);
            newLayer.setPositionY(this.positionY);

            return newLayer;
        }catch (CloneNotSupportedException e) {
            throw new RuntimeException("Клонування не підтримується");
        }
    }
}