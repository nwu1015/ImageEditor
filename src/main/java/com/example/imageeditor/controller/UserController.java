package com.example.imageeditor.controller;

import com.example.imageeditor.entity.User;
import com.example.imageeditor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {
        try {
            userService.register(user.getLogin(), user.getPassword());
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String login, @RequestParam String password, Model model) {
        try {
            userService.login(login, password);
            return "redirect:/images";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }
}

