package com.example.imageeditor.repository;

import com.example.imageeditor.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
