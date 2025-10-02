package com.example.imageeditor.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "compress_effects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompressEffect extends Effect {
    private int quality; // 0 - 100
}
