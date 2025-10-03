package com.example.imageeditor.repository;

import com.example.imageeditor.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {
    //List<Image> findByUserId(int userId);
}
