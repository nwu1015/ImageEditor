package com.example.imageeditor.repository;

import com.example.imageeditor.domain.Collage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollageRepository extends JpaRepository<Collage, Long> {
}
