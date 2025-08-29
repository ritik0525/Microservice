package com.example.upload.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtService {

    private final Algorithm algorithm;
    private final long accessExpirationSeconds;

    public JwtService(@Value("${security.jwt.access-secret}") String secret,
                      @Value("${security.jwt.access-expiration-seconds:900}") long accessExpirationSeconds) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.accessExpirationSeconds = accessExpirationSeconds;
    }

    public String generateAccessToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpirationSeconds * 1000);
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(now)
                .withExpiresAt(exp)
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        return JWT.require(algorithm).build().verify(token);
    }

    public String getUsername(String token) {
        DecodedJWT djwt = verify(token);
        return djwt.getSubject();
    }
}