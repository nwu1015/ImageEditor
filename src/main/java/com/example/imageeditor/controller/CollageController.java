package com.example.imageeditor.controller;

import com.example.imageeditor.entity.Collage;
import com.example.imageeditor.service.CollageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collages")
@RequiredArgsConstructor
public class CollageController {

    private final CollageService collageService;

    // створення колажу (userId + список imageId)
    @PostMapping("/create/{userId}")
    public ResponseEntity<Collage> createCollage(@RequestBody CollageRequest request,
                                                 @PathVariable int userId) {
        Collage savedCollage = collageService.createCollage(request.toEntity(), userId, request.getImageIds());
        return ResponseEntity.ok(savedCollage);
    }

    // отримати всі колажі користувача
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Collage>> getUserCollages(@PathVariable int userId) {
        return ResponseEntity.ok(collageService.getCollagesByUser(userId));
    }

    // отримати конкретний колаж
    @GetMapping("/{id}")
    public ResponseEntity<Collage> getCollageById(@PathVariable int id) {
        return collageService.getCollageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // видалити колаж
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollage(@PathVariable int id) {
        collageService.deleteCollage(id);
        return ResponseEntity.noContent().build();
    }

    // DTO-шка для запиту
    public static class CollageRequest {
        private String name;
        private List<Integer> imageIds;

        public Collage toEntity() {
            Collage collage = new Collage();
            collage.setName(this.name);
            return collage;
        }

        public String getName() {
            return name;
        }

        public List<Integer> getImageIds() {
            return imageIds;
        }
    }
}
