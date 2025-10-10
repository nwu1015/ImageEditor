package com.example.imageeditor.repository;

import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageLayerRepository extends JpaRepository<ImageLayer, Long> {
    Optional<ImageLayer> findById(Long id);

    Optional<ImageLayer> findFirstByImageAndCollage_User(Image image, User user);

    List<ImageLayer> findAllByImage(Image image);
}
