package com.example.imageeditor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "effects")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Effect {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "image_id")
    private Image image;
}
