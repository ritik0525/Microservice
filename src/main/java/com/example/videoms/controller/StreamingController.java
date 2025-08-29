package com.example.videoms.controller;

import com.example.videoms.service.VideoProcessingService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.PathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/stream")
public class StreamingController {

    private final VideoProcessingService processingService;

    public StreamingController(VideoProcessingService processingService) {
        this.processingService = processingService;
    }

    @GetMapping("/hls/{id}/{filename:.+}")
    public ResponseEntity<Resource> hls(@PathVariable String id, @PathVariable String filename) {
        Path file = processingService.getVideoDir(id).resolve(filename);
        return serveFile(file);
    }

    @GetMapping("/hls/{id}")
    public ResponseEntity<Resource> hlsDefault(@PathVariable String id) {
        Path file = processingService.getVideoDir(id).resolve("hls_master.m3u8");
        return serveFile(file);
    }

    @GetMapping("/dash/{id}/{filename:.+}")
    public ResponseEntity<Resource> dash(@PathVariable String id, @PathVariable String filename) {
        Path file = processingService.getVideoDir(id).resolve(filename);
        return serveFile(file);
    }

    @GetMapping("/dash/{id}")
    public ResponseEntity<Resource> dashDefault(@PathVariable String id) {
        Path file = processingService.getVideoDir(id).resolve("manifest.mpd");
        return serveFile(file);
    }

    private ResponseEntity<Resource> serveFile(Path path) {
        try {
            PathResource res = new PathResource(path);
            if (!res.exists() || !res.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            MediaType contentType = detectContentType(path.getFileName().toString());
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .body(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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