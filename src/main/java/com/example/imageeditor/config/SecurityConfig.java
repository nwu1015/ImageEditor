package com.example.imageeditor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig{

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Дозволяємо доступ всім до головної сторінки, сторінок реєстрації та логіну, а також до статичних ресурсів
                        .requestMatchers("/", "/register", "/login", "/css/**", "/js/**").permitAll()
                        // Всі інші запити вимагають, щоб користувач був аутентифікований
                        .anyRequest().authenticated()
                )
                // Вказуємо Spring Security використовувати стандартну форму логіну
                .formLogin(form -> form
                        .loginPage("/login") // Наша кастомна сторінка для входу
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // URL, який обробляє вихід
                        .logoutSuccessUrl("/") // Куди перенаправити після виходу
                        .permitAll()
                );

        return http.build();
    }
}
