package com.example.imageeditor.controller;

import com.example.imageeditor.domain.*;
import com.example.imageeditor.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
@RequestMapping("/editor")
public class CollageController {

    @Autowired
    private CollageService collageService;

    /**
     * Відображає головну сторінку редактора для конкретного колажу.
     */
    @GetMapping("/{collageId}")
    public String showEditorPage(@PathVariable Long collageId, Model model) {
        model.addAttribute("collage", collageService.findCollageById(collageId));
        return "editor"; // Повертає назву HTML-файлу: "editor.html"
    }

    /**
     * Обробляє завантаження нового зображення.
     */
    @PostMapping("/{collageId}/upload")
    public String handleImageUpload(@PathVariable Long collageId, @RequestParam("imageFile") MultipartFile file) {
        try {
            collageService.addImageToCollage(collageId, file);
        } catch (IOException e) {
            // Тут варто додати обробку помилок, наприклад, через flash-атрибути
            e.printStackTrace();
        }
        // Перенаправляє користувача на ту саму сторінку, щоб оновити вигляд
        return "redirect:/editor/" + collageId;
    }

    /**
     * Обробляє дії над існуючим шаром (поворот, видалення і т.д.).
     */
    @PostMapping("/{collageId}/layer-action")
    public String handleLayerAction(@PathVariable Long collageId,
                                    @RequestParam Long layerId,
                                    @RequestParam String action) {
        collageService.updateLayerAction(layerId, action);
        return "redirect:/editor/" + collageId;
    }
}
