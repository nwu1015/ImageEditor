package com.example.imageeditor.repository;

import com.example.imageeditor.domain.Collage;
import com.example.imageeditor.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollageRepository extends JpaRepository<Collage, Long> {
    List<Collage> findByUserOrderByLastModifiedAtDesc(User user);
}
