package com.example.videoms.controller;

import com.example.videoms.model.Video;
import com.example.videoms.repository.VideoRepository;
import com.example.videoms.service.VideoProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@RestController
@RequestMapping("/api/videos")
public class UploadController {

    private final VideoRepository videoRepository;
    private final VideoProcessingService processingService;

    @Value("${video.storage.path:videos}")
    private String storageRoot;

    public UploadController(VideoRepository videoRepository, VideoProcessingService processingService) {
        this.videoRepository = videoRepository;
        this.processingService = processingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Video v = new Video();
        v.setOriginalFilename(file.getOriginalFilename());
        v.setContentType(file.getContentType());
        v.setSize(file.getSize());
        v.setStatus(Video.Status.UPLOADED);
        videoRepository.save(v);

        // store temporarily then kick off async processing
        Path root = Paths.get(storageRoot);
        Files.createDirectories(root);
        Path tmpFile = Files.createTempFile(root, v.getId() + "-", ".upload");
        Files.write(tmpFile, file.getBytes(), StandardOpenOption.WRITE);

        processingService.processVideoAsync(v.getId(), tmpFile);

        return ResponseEntity.ok().body(new UploadResponse(v.getId(), "/api/videos/" + v.getId() + "/status"));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> status(@PathVariable String id) {
        return videoRepository.findById(id)
                .map(v -> ResponseEntity.ok(new StatusResponse(v.getId(), v.getStatus().name(), v.getFailureReason())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Video not found"));
    }

    // DTOs
    record UploadResponse(String id, String statusUrl) {}
    record StatusResponse(String id, String status, String reason) {}
}