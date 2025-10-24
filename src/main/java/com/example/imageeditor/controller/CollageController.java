package com.example.imageeditor.controller;

import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.ImageRepository;
import com.example.imageeditor.service.CollageService;
import com.example.imageeditor.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/collages")
@RequiredArgsConstructor
public class CollageController {

    private final CollageService collageService;

    private final ImageRepository imageRepository;

    private final ImageService imageService;

    /**
     * Transition method: finds the collage by image ID and redirects to the main editor.
     */
    @GetMapping("/from-image/{imageId}")
    public String editCollageFromImage(@PathVariable Long imageId, @AuthenticationPrincipal User user) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found!"));

        if (!image.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Access Denied");
        }

        ImageLayer layer = collageService.findOrCreateLayerForImage(image, user);
        Long collageId = layer.getCollage().getId();

        return "redirect:/collages/" + collageId;
    }

    /**
     * The main method that displays the editor page for a specific collage.
     */
    @GetMapping("/{collageId}")
    public String showEditorPage(@PathVariable Long collageId, Model model) {
        model.addAttribute("collage", collageService.findCollageById(collageId));
        return "editor";
    }

    /**
     * Handles loading a new image into the collage.
     */
    @PostMapping("/{collageId}/layers/add")
    public String handleImageUpload(@PathVariable Long collageId,
                                    @RequestParam("imageFile") MultipartFile file,
                                    @AuthenticationPrincipal User user,
                                    RedirectAttributes redirectAttributes) {
        try {
            collageService.addImageToCollage(collageId, file, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Зображення додано!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Помилка завантаження файлу.");
            e.printStackTrace();
        }
        return "redirect:/collages/" + collageId;
    }

    /**
     * Processes actions on an existing layer (rotate, delete, etc.).
     */
    @PostMapping("/{collageId}/layers/{layerId}/action")
    public String handleLayerAction(@PathVariable Long collageId,
                                    @PathVariable Long layerId,
                                    @RequestParam String action) {
        collageService.updateLayerAction(layerId, action);
        return "redirect:/collages/" + collageId;
    }

    @GetMapping("/layers/{layerId}/transformed")
    @ResponseBody
    public ResponseEntity<Resource> getTransformedLayer(@PathVariable Long layerId) {
        try {
            Resource resource = imageService.getTransformedLayerAsResource(layerId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue())
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{collageId}/render")
    public String renderCollage(@PathVariable Long collageId, @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        try {
            Image finalImage = collageService.renderAndSaveCollage(collageId, user);
            redirectAttributes.addFlashAttribute("finalImageId", finalImage.getId());
            return "redirect:/my-images";
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Не вдалося зберегти колаж.");
            return "redirect:/collages/" + collageId;
        }
    }

    @PostMapping("/{collageId}/layers/{layerId}/update")
    public String updateLayerDetails(@PathVariable Long collageId,
                                     @PathVariable Long layerId,
                                     @ModelAttribute CollageService.LayerUpdateDTO dto) {
        collageService.updateImageLayer(layerId, dto);
        return "redirect:/collages/" + collageId;
    }

    @PostMapping("/{collageId}/layers/{layerId}/clone")
    public String cloneLayer(@PathVariable Long collageId,
                                     @PathVariable Long layerId,
                                     @ModelAttribute CollageService.LayerUpdateDTO dto) {
        collageService.duplicateLayer(layerId);
        collageService.updateImageLayer(layerId, dto);
        return "redirect:/collages/" + collageId;
    }
}