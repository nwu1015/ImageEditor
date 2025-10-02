package com.example.imageeditor.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class CropEffect extends Effect {
    private int x;
    private int y;
    private int width;
    private int height;
}
