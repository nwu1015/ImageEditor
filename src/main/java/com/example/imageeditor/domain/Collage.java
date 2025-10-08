package com.example.imageeditor.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collages")
@Data
@NoArgsConstructor
public class Collage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private int canvasWidth;  // Ширина полотна для колажу
    private int canvasHeight; // Висота полотна для колажу

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastModifiedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "collage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("zIndex ASC") // Шари будуть завантажуватися впорядкованими
    private List<ImageLayer> layers = new ArrayList<>();
}
