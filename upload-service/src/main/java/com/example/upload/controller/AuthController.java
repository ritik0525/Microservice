package com.example.upload.controller;

import com.example.upload.service.UserService;
import com.example.upload.service.RefreshTokenService;
import com.example.upload.model.RefreshToken;
import com.example.upload.model.User;
import com.example.upload.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    record RegisterRequest(String username, String password) {}
    record LoginRequest(String username, String password) {}
    record RefreshRequest(String refreshToken) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            User u = userService.register(req.username(), req.password());
            return ResponseEntity.status(201).body(Map.of("id", u.getId(), "username", u.getUsername()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );
            // generate tokens
            String accessToken = jwtService.generateAccessToken(req.username());
            User u = userService.findByUsername(req.username()).orElseThrow();
            RefreshToken rt = refreshTokenService.createRefreshToken(u.getId());
            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "accessExpiresIn", Integer.parseInt(System.getProperty("security.jwt.access-expiration-seconds", "900")),
                    "refreshToken", rt.getToken(),
                    "refreshExpiresAt", rt.getExpiresAt().toString()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        try {
            RefreshToken existing = refreshTokenService.findByToken(req.refreshToken())
                    .orElseThrow(() -> new IllegalArgumentException("invalid_refresh_token"));
            refreshTokenService.verifyNotExpired(existing);
            // rotate refresh token
            RefreshToken newRt = refreshTokenService.rotate(existing);
            String username = existing.getUser().getUsername();
            String newAccess = jwtService.generateAccessToken(username);
            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccess,
                    "accessExpiresIn", Integer.parseInt(System.getProperty("security.jwt.access-expiration-seconds", "900")),
                    "refreshToken", newRt.getToken(),
                    "refreshExpiresAt", newRt.getExpiresAt().toString()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", "server_error"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshRequest req) {
        try {
            refreshTokenService.findByToken(req.refreshToken()).ifPresent(rt -> refreshTokenService.revokeToken(rt));
            return ResponseEntity.ok(Map.of("status","logged_out"));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error","server_error"));
        }
    }
}