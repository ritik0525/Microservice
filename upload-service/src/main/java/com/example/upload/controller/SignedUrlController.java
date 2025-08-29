package com.example.upload.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class SignedUrlController {

    @Value("${signedurl.secret}")
    private String signedUrlSecret;

    @Value("${signedurl.default-ttl-seconds:300}")
    private long defaultTtl;

    @Value("${cdn.base.url:http://localhost:8084}")
    private String cdnBaseUrl;

    // Generate a signed URL for a given video id and type (hls/dash)
    @PostMapping("/{id}/signed-url")
    public ResponseEntity<?> signedUrl(@PathVariable String id,
                                       @RequestParam(defaultValue = "hls") String type,
                                       @RequestParam(required = false) Long ttlSeconds,
                                       Authentication authentication,
                                       HttpServletRequest request) throws Exception {
        // simple auth check: JwtAuthenticationFilter must have set authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
        }

        long ttl = (ttlSeconds == null) ? defaultTtl : ttlSeconds;
        long expires = Instant.now().getEpochSecond() + ttl;

        String path;
        if ("dash".equalsIgnoreCase(type)) {
            path = String.format("/stream/dash/%s/manifest.mpd", id);
        } else {
            path = String.format("/stream/hls/%s/hls_master.m3u8", id);
        }

        String sig = sign(path, expires, signedUrlSecret);

        String url = cdnBaseUrl + path + "?expires=" + expires + "&sig=" + URLEncoder.encode(sig, StandardCharsets.UTF_8);

        return ResponseEntity.ok(Map.of("url", url, "expires", expires));
    }

    private static String sign(String path, long expires, String secret) throws Exception {
        String payload = path + ":" + expires;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}