package com.example.imageeditor.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "image_layers")
@Data
@NoArgsConstructor
public class ImageLayer {

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
}