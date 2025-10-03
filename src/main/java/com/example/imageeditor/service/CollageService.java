package com.example.imageeditor.service;

import com.example.imageeditor.entity.Collage;
import com.example.imageeditor.entity.Image;
import com.example.imageeditor.entity.User;
import com.example.imageeditor.repository.CollageRepository;
import com.example.imageeditor.repository.ImageRepository;
import com.example.imageeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CollageService {

    private final CollageRepository collageRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;

    public Collage createCollage(Collage collage, int userId, List<Integer> imageIds) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        List<Image> images = imageRepository.findAllById(imageIds);

        collage.setOwner(userOpt.get());
        collage.setImages(images);

        return collageRepository.save(collage);
    }

    public List<Collage> getCollagesByUser(int userId) {
        return collageRepository.findAll()
                .stream()
                .filter(c -> c.getOwner() != null && c.getOwner().getId() == userId)
                .toList();
    }

    public Optional<Collage> getCollageById(int id) {
        return collageRepository.findById(id);
    }

    public void deleteCollage(int id) {
        collageRepository.deleteById(id);
    }
}
