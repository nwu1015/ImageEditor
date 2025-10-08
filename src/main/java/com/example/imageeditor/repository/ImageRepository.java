package com.example.imageeditor.repository;

import com.example.imageeditor.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
