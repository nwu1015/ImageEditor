package com.example.imageeditor.controller;

import com.example.imageeditor.domain.User;
import com.example.imageeditor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") User user) { // Приймаємо об'єкт User
        try {
            userService.register(user);
        } catch (IllegalStateException e) {
            // Якщо користувач вже існує, повертаємо на сторінку з помилкою
            return "redirect:/register?error";
        }
        return "redirect:/login?success";
    }
}
