package com.example.imageeditor.controller;

import com.example.imageeditor.entity.Image;
import com.example.imageeditor.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload/{userId}")
    public ResponseEntity<Image> uploadImage(@RequestBody Image image,
                                             @PathVariable int userId) {
        Image savedImage = imageService.saveImage(image, userId);
        return ResponseEntity.ok(savedImage);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Image>> getUserImages(@PathVariable int userId) {
        return ResponseEntity.ok(imageService.getImagesByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Image> getImageById(@PathVariable int id) {
        return imageService.getImageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable int id) {
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
