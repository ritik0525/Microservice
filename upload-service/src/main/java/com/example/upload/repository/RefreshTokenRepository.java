package com.example.upload.repository;

import com.example.upload.model.RefreshToken;
import com.example.upload.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    void deleteAllByUser(User user);
}