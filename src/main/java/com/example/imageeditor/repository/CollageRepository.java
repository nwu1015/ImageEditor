package com.example.imageeditor.repository;

import com.example.imageeditor.entity.Collage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollageRepository extends JpaRepository<Collage, Integer> {
    List<Collage> findByUserId(int userId);
}
