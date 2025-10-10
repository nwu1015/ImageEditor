package com.example.imageeditor.repository;

import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByOwner(User owner);
}
