package com.example.upload.service;

import com.example.upload.model.RefreshToken;
import com.example.upload.model.User;
import com.example.upload.repository.RefreshTokenRepository;
import com.example.upload.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.security.SecureRandom;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final long refreshTtlSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokenRepo,
                               UserRepository userRepo,
                               @Value("${security.jwt.refresh-ttl-seconds:1209600}") long refreshTtlSeconds) {
        this.tokenRepo = tokenRepo;
        this.userRepo = userRepo;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public RefreshToken createRefreshToken(String userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("user_not_found"));
        // opaque token (random)
        String token = new BigInteger(256, secureRandom).toString(36) + "-" + UUID.randomUUID();
        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds));
        rt.setRevoked(false);
        return tokenRepo.save(rt);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return tokenRepo.findByToken(token);
    }

    public RefreshToken verifyNotExpired(RefreshToken token) {
        if (token.getExpiresAt().isBefore(Instant.now()) || token.isRevoked()) {
            throw new IllegalArgumentException("refresh_token_expired_or_revoked");
        }
        return token;
    }

    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        tokenRepo.save(token);
    }

    public void revokeAllForUser(User user) {
        tokenRepo.deleteAllByUser(user);
    }

    public RefreshToken rotate(RefreshToken existing) {
        // revoke existing and create new for same user
        existing.setRevoked(true);
        tokenRepo.save(existing);
        return createRefreshToken(existing.getUser().getId());
    }
}