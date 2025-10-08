package com.example.imageeditor.repository;

import com.example.imageeditor.domain.ImageLayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageLayerRepository extends JpaRepository<ImageLayer, Long> {
}
