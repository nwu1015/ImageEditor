package com.example.imageeditor.repository;

import com.example.imageeditor.domain.Collage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollageRepository extends JpaRepository<Collage, Long> {
}
