package com.example.videoms.service;

import com.example.videoms.model.Video;
import com.example.videoms.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessingService {

    private final VideoRepository videoRepository;

    @Value("${video.storage.path:videos}")
    private String storageRoot;

    public VideoProcessingService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    public Path getVideoDir(String id) {
        return Paths.get(storageRoot).resolve(id);
    }

    @Async
    @Transactional
    public void processVideoAsync(String videoId, Path uploadedFile) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) return;
        video.setStatus(Video.Status.PROCESSING);
        videoRepository.save(video);

        try {
            Path videoDir = getVideoDir(videoId);
            Files.createDirectories(videoDir);

            // Move uploaded file into working location
            Path input = videoDir.resolve("input" + getExtension(uploadedFile.getFileName().toString()));
            Files.move(uploadedFile, input, StandardCopyOption.REPLACE_EXISTING);

            // run FFmpeg commands to generate HLS and DASH
            generateHls(input, videoDir);
            generateDash(input, videoDir);

            video.setStatus(Video.Status.READY);
            video.setProcessedAt(Instant.now());
            videoRepository.save(video);
        } catch (Exception ex) {
            video.setStatus(Video.Status.FAILED);
            video.setFailureReason(ex.getMessage());
            videoRepository.save(video);
        }
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }

    private void generateHls(Path input, Path outDir) throws IOException, InterruptedException {
        // Simple single-bitrate HLS. For adaptive stream create multiple renditions and a master playlist.
        Path playlist = outDir.resolve("hls_master.m3u8");
        String segPattern = outDir.resolve("hls_seg_%03d.ts").toString();

        List<String> cmd = Arrays.asList(
                "ffmpeg", "-y", "-i", input.toString(),
                "-preset", "veryfast",
                "-c:v", "libx264", "-profile:v", "main",
                "-crf", "23", "-sc_threshold", "0",
                "-g", "48",
                "-c:a", "aac", "-b:a", "128k",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segPattern,
                playlist.toString()
        );

        runCommand(cmd, outDir.toFile());
    }

    private void generateDash(Path input, Path outDir) throws IOException, InterruptedException {
        Path manifest = outDir.resolve("manifest.mpd");

        List<String> cmd = Arrays.asList(
                "ffmpeg", "-y", "-i", input.toString(),
                "-c:v", "libx264", "-profile:v", "main",
                "-crf", "23", "-sc_threshold", "0", "-g", "48",
                "-c:a", "aac", "-b:a", "128k",
                "-use_timeline", "1", "-use_template", "1",
                "-f", "dash", manifest.toString()
        );

        runCommand(cmd, outDir.toFile());
    }

    private void runCommand(List<String> cmd, File workingDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean finished = p.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("FFmpeg timed out");
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("FFmpeg failed with exit code " + p.exitValue());
        }
    }
}