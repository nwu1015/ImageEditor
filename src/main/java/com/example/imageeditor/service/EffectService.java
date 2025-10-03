package com.example.imageeditor.service;

import com.example.imageeditor.entity.Effect;
import com.example.imageeditor.entity.Image;
import com.example.imageeditor.repository.EffectRepository;
import com.example.imageeditor.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EffectService {

    private final EffectRepository effectRepository;
    private final ImageRepository imageRepository;

    public Effect addEffectToImage(Effect effect, int imageId) {
        Optional<Image> imgOpt = imageRepository.findById(imageId);
        if (imgOpt.isEmpty()) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }
        effect.setImage(imgOpt.get());
        return effectRepository.save(effect);
    }

    public List<Effect> getEffectsByImage(int imageId) {
        return effectRepository.findAll()
                .stream()
                .filter(e -> e.getImage() != null && e.getImage().getId() == imageId)
                .toList();
    }

    public Optional<Effect> getEffectById(int id) {
        return effectRepository.findById(id);
    }

    public void deleteEffect(int id) {
        effectRepository.deleteById(id);
    }
}
