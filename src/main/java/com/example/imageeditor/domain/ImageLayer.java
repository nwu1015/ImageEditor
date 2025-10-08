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

    // Зв'язок з проектом, до якого належить цей шар
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Collage collage;

    // Зв'язок з оригінальним зображенням, яке використовується у шарі
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    // --- Властивості трансформації ---

    private int positionX; // Координата X на полотні
    private int positionY; // Координата Y на полотні

    private int width;     // Ширина шару (може відрізнятися від оригіналу через розтягування)
    private int height;    // Висота шару

    private double rotationAngle = 0.0; // Кут повороту в градусах

    private int zIndex; // Порядок шару (0 - найнижчий, вищі значення - ближче до глядача)

    // Параметри кадрування (якщо є)
    private Integer cropX;
    private Integer cropY;
    private Integer cropWidth;
    private Integer cropHeight;

    // Інші ефекти можна зберігати у вигляді JSON-рядка для гнучкості
    @Column(columnDefinition = "TEXT")
    private String effectsJson; // Наприклад: {"brightness": 1.2, "contrast": 0.8}
}