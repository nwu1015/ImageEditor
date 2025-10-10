package com.example.imageeditor.service;

import com.example.imageeditor.domain.User;
import com.example.imageeditor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(User user) {
        // Перевіряємо, чи користувач з таким іменем вже не існує
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalStateException("User already exists!");
        }

        // Хешуємо пароль перед збереженням
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // Зберігаємо користувача в базу даних
        userRepository.save(user);
    }
}
