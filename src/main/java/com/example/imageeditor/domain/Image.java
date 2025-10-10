package com.example.imageeditor.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@Data
@NoArgsConstructor
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column
    private String title;

    // Шлях до файлу у файловій системі або URL у хмарному сховищі
    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String fileFormat;

    private int width;
    private int height;

    @Column(updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
}