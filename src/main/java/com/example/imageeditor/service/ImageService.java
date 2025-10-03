package com.example.imageeditor.service;

import com.example.imageeditor.entity.Image;
import com.example.imageeditor.entity.User;
import com.example.imageeditor.repository.ImageRepository;
import com.example.imageeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    public Image saveImage(Image image, int userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        image.setOwner(userOpt.get());
        return imageRepository.save(image);
    }

    public List<Image> getImagesByUser(int userId) {
        return imageRepository.findAll()
                .stream()
                .filter(img -> img.getOwner() != null && img.getOwner().getId() == userId)
                .toList();
    }

    public Optional<Image> getImageById(int id) {
        return imageRepository.findById(id);
    }

    public void deleteImage(int id) {
        imageRepository.deleteById(id);
    }
}
