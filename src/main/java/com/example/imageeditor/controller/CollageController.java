package com.example.imageeditor.controller;

import com.example.imageeditor.domain.Image;
import com.example.imageeditor.domain.ImageLayer;
import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.ImageRepository;
import com.example.imageeditor.service.CollageService;
import com.example.imageeditor.service.ImageService;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/collages") // 1. Змінили базовий URL на логічний "/collages"
public class CollageController {

    @Autowired
    private CollageService collageService;
    @Autowired
    private ImageRepository imageRepository; // Потрібен для методу-перехідника

    @Autowired
    private ImageService imageService;

    /**
     * Метод-перехідник: знаходить колаж за ID зображення і перенаправляє на основний редактор.
     * Сюди буде вести посилання "Редагувати" з галереї.
     */
    @GetMapping("/from-image/{imageId}")
    public String editCollageFromImage(@PathVariable Long imageId, @AuthenticationPrincipal User user) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found!"));

        // Перевірка власності
        if (!image.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("Access Denied");
        }

        ImageLayer layer = collageService.findOrCreateLayerForImage(image, user);
        Long collageId = layer.getCollage().getId();

        // Перенаправляємо на головний ендпоінт редактора
        return "redirect:/collages/" + collageId;
    }

    /**
     * Головний метод, що відображає сторінку редактора для конкретного колажу.
     */
    @GetMapping("/{collageId}")
    public String showEditorPage(@PathVariable Long collageId, Model model) {
        // Перевірка власності колажу (добра практика - додати в сервіс)
        model.addAttribute("collage", collageService.findCollageById(collageId));
        return "editor"; // Повертає editor.html
    }

    /**
     * Обробляє завантаження нового зображення в колаж.
     */
    @PostMapping("/{collageId}/layers/add") // 2. Зробили URL більш чітким
    public String handleImageUpload(@PathVariable Long collageId,
                                    @RequestParam("imageFile") MultipartFile file,
                                    @AuthenticationPrincipal User user,
                                    RedirectAttributes redirectAttributes) {
        try {
            collageService.addImageToCollage(collageId, file, user);
            redirectAttributes.addFlashAttribute("successMessage", "Зображення додано!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Помилка завантаження файлу.");
            e.printStackTrace();
        }
        return "redirect:/collages/" + collageId;
    }

    /**
     * Обробляє дії над існуючим шаром (поворот, видалення і т.д.).
     */
    @PostMapping("/{collageId}/layers/{layerId}/action") // 3. Зробили URL більш RESTful
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
            // Використовуємо ImageService для логіки трансформації
            Resource resource = imageService.getTransformedLayerAsResource(layerId);
            return ResponseEntity.ok().body(resource);
        } catch (Exception e) {
            // Якщо сталася помилка, повертаємо 404, щоб браузер знав, що файлу немає
            e.printStackTrace(); // Допоможе побачити помилку в консолі сервера
            return ResponseEntity.notFound().build();
        }
    }

    // === ДОДАЙТЕ ЦЕЙ МЕТОД ===
    @PostMapping("/{collageId}/render")
    public String renderCollage(@PathVariable Long collageId, @AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        try {
            Image finalImage = collageService.renderAndSaveCollage(collageId, user);
            // Передаємо ID фінального зображення, щоб показати його на сторінці результату
            redirectAttributes.addFlashAttribute("finalImageId", finalImage.getId());
            return "redirect:/my-images"; // Перенаправляємо в галерею, де з'явиться новий файл
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Не вдалося зберегти колаж.");
            return "redirect:/collages/" + collageId;
        }
    }
}