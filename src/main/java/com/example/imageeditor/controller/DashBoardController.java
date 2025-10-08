package com.example.imageeditor.controller;

import com.example.imageeditor.service.CollageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashBoardController {

    @Autowired
    private CollageService collageService;

    /**
     * Відображає головну сторінку зі списком усіх колажів.
     */
    @GetMapping("/") // Робимо цю сторінку кореневою
    public String showDashboard(Model model) {
        model.addAttribute("collages", collageService.findAllCollages());
        return "dashboard"; // Назва HTML-файлу: dashboard.html
    }

    /**
     * Обробляє створення нового колажу.
     */
    @PostMapping("/create-collage")
    public String createNewCollage(@RequestParam String name,
                                   @RequestParam int canvasWidth,
                                   @RequestParam int canvasHeight) {
        // УВАГА: В реальному додатку ID користувача має братися із сесії/контексту безпеки.
        // Для прикладу ми тимчасово використовуємо ID=1.
        final Long CURRENT_USER_ID = 1L;

        collageService.createCollage(name, canvasWidth, canvasHeight, CURRENT_USER_ID);

        return "redirect:/"; // Перенаправляємо назад на головну сторінку
    }
}
