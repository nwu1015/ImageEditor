package com.example.imageeditor.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ScaleEffect extends Effect {
    private int width;
    private int height;
}
