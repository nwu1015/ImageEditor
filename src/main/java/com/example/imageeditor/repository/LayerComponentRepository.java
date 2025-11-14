package com.example.imageeditor.repository;

import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.LayerComponent;
import com.example.imageeditor.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LayerComponentRepository extends JpaRepository<LayerComponent, Long> {

    @Query("SELECT il FROM ImageLayer il WHERE il.id = :id")
    Optional<ImageLayer> findImageLayerById(@Param("id") Long id);

    @Query("SELECT il FROM ImageLayer il WHERE il.image = :image AND il.collage.user = :user")
    Optional<ImageLayer> findFirstImageLayerByImageAndUser(
            @Param("image") Image image,
            @Param("user") User user
    );

    @Query("SELECT il FROM ImageLayer il WHERE il.image = :image")
    List<ImageLayer> findAllImageLayersByImage(@Param("image") Image image);
}