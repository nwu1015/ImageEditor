package com.example.imageeditor.service;

import com.example.imageeditor.entity.User;
import com.example.imageeditor.entity.UserRole;
import com.example.imageeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String login, String rawPassword) {
        if (userRepository.findByLogin(login).isPresent()) {
            throw new IllegalArgumentException("Користувач з таким логіном вже існує");
        }
        User user = User.builder()
                .login(login)
                .password(passwordEncoder.encode(rawPassword))
                .role(UserRole.REGISTERED)
                .build();
        return userRepository.save(user);
    }

    public User login(String login, String rawPassword) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Невірний логін або пароль"));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Невірний логін або пароль");
        }
        return user;
    }
}

