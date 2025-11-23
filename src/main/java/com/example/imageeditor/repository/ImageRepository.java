package com.example.imageeditor.repository;

import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByOwner(User owner);

    @Query("SELECT i FROM Image i WHERE i.owner = :owner AND i.renderedResult = false")
    List<Image> findByUserAndRenderedResultFalse(@Param("owner") User owner);
}