package com.example.imageeditor.controller;

import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.ImageRepository;
import com.example.imageeditor.service.CollageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class EditorController {

    private final ImageRepository imageRepository;
    private final CollageService collageService;

    @GetMapping("/editor/{imageId}")
    public String showEditor(@PathVariable Long imageId, Model model, @AuthenticationPrincipal User user) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found!"));

        if (!image.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Access Denied");
        }

        ImageLayer layer = collageService.findOrCreateLayerForImage(image, user);

        model.addAttribute("layer", layer);
        return "editor";
    }

    @PostMapping("/layers/{layerId}/update")
    public String updateLayer(@PathVariable Long layerId, CollageService.LayerUpdateDTO dto) {
        ImageLayer updatedLayer = collageService.updateImageLayer(layerId, dto);
        // Повертаємо користувача назад на сторінку редактора
        return "redirect:/editor/" + updatedLayer.getImage().getId();
    }
}