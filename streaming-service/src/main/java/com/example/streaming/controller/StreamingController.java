package com.example.streaming.controller;

import org.springframework.core.io.PathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Base64;

@RestController
@RequestMapping("/stream")
public class StreamingController {

    private final Path storageRoot;
    private final String signingSecret;

    public StreamingController(@org.springframework.beans.factory.annotation.Value("${video.storage.path:/data/videos}") String storagePath,
                               @org.springframework.beans.factory.annotation.Value("${signedurl.secret}") String signingSecret) {
        this.storageRoot = Paths.get(storagePath);
        this.signingSecret = signingSecret;
    }

    @GetMapping("/hls/{id}/{filename:.+}")
    public ResponseEntity<?> hlsFile(@PathVariable String id, @PathVariable String filename,
                                     @RequestParam(required = false) Long expires,
                                     @RequestParam(required = false) String sig) {
        String requestPath = String.format("/stream/hls/%s/%s", id, filename);
        if (!validateSignature(requestPath, expires, sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or expired signature");
        }
        return serveFile(id, filename);
    }

    @GetMapping("/hls/{id}")
    public ResponseEntity<?> hlsDefault(@PathVariable String id,
                                        @RequestParam(required = false) Long expires,
                                        @RequestParam(required = false) String sig) {
        String requestPath = String.format("/stream/hls/%s/hls_master.m3u8", id);
        if (!validateSignature(requestPath, expires, sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or expired signature");
        }
        return serveFile(id, "hls_master.m3u8");
    }

    @GetMapping("/dash/{id}/{filename:.+}")
    public ResponseEntity<?> dashFile(@PathVariable String id, @PathVariable String filename,
                                      @RequestParam(required = false) Long expires,
                                      @RequestParam(required = false) String sig) {
        String requestPath = String.format("/stream/dash/%s/%s", id, filename);
        if (!validateSignature(requestPath, expires, sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or expired signature");
        }
        return serveFile(id, filename);
    }

    @GetMapping("/dash/{id}")
    public ResponseEntity<?> dashDefault(@PathVariable String id,
                                         @RequestParam(required = false) Long expires,
                                         @RequestParam(required = false) String sig) {
        String requestPath = String.format("/stream/dash/%s/manifest.mpd", id);
        if (!validateSignature(requestPath, expires, sig)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or expired signature");
        }
        return serveFile(id, "manifest.mpd");
    }

    private boolean validateSignature(String path, Long expires, String sig) {
        try {
            if (expires == null || sig == null) return false;
            long now = Instant.now().getEpochSecond();
            if (expires < now) return false;
            String payload = path + ":" + expires;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes());
            String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
            return expected.equals(sig);
        } catch (Exception e) {
            return false;
        }
    }

    private ResponseEntity<?> serveFile(String id, String filename) {
        try {
            Path p = storageRoot.resolve(id).resolve(filename);
            PathResource res = new PathResource(p);
            if (!res.exists() || !res.isReadable()) return ResponseEntity.notFound().build();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(detectContentType(filename));
            return new ResponseEntity<>(res, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private MediaType detectContentType(String name) {
        if (name.endsWith(".m3u8")) return MediaType.valueOf("application/vnd.apple.mpegurl");
        if (name.endsWith(".ts")) return MediaType.valueOf("video/MP2T");
        if (name.endsWith(".mpd")) return MediaType.valueOf("application/dash+xml");
        if (name.endsWith(".mp4")) return MediaType.valueOf("video/mp4");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}