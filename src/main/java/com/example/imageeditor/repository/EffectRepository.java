package com.example.imageeditor.repository;

import com.example.imageeditor.entity.Effect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EffectRepository extends JpaRepository<Effect, Integer> {
    List<Effect> findByImageId(int imageId);
}
