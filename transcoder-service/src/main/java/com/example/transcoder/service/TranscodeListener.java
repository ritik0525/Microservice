package com.example.transcoder.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.nio.file.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.util.Arrays;

@Component
public class TranscodeListener {

    @Value("${upload.service.url:http://upload-service:8081}")
    private String uploadServiceUrl;

    private final RestTemplate rest = new RestTemplate();

    @RabbitListener(queues = "video.uploaded")
    public void onMessage(Map<String,Object> msg) {
        String id = (String) msg.get("id");
        String path = (String) msg.get("path");
        try {
            // mark processing
            rest.postForEntity(uploadServiceUrl + "/api/videos/" + id + "/status", Map.of("status","PROCESSING"), Void.class);

            Path input = Paths.get(path);
            Path videoDir = input.getParent();
            Files.createDirectories(videoDir);

            // generate HLS
            Path hlsPlaylist = videoDir.resolve("hls_master.m3u8");
            String tsPattern = videoDir.resolve("hls_seg_%03d.ts").toString();
            List<String> hlsCmd = Arrays.asList(
                    "ffmpeg", "-y", "-i", input.toString(),
                    "-preset", "veryfast", "-c:v", "libx264", "-profile:v", "main",
                    "-crf", "23","-sc_threshold","0","-g","48",
                    "-c:a","aac","-b:a","128k",
                    "-hls_time","6","-hls_playlist_type","vod",
                    "-hls_segment_filename", tsPattern,
                    hlsPlaylist.toString()
            );
            run(hlsCmd, videoDir.toFile());

            // generate DASH
            Path dashManifest = videoDir.resolve("manifest.mpd");
            List<String> dashCmd = Arrays.asList(
                    "ffmpeg", "-y", "-i", input.toString(),
                    "-c:v", "libx264", "-profile:v", "main",
                    "-crf", "23","-sc_threshold","0","-g","48",
                    "-c:a","aac","-b:a","128k",
                    "-use_timeline","1","-use_template","1","-f","dash", dashManifest.toString()
            );
            run(dashCmd, videoDir.toFile());

            // notify upload service ready
            rest.postForEntity(uploadServiceUrl + "/api/videos/" + id + "/status", Map.of("status","READY"), Void.class);
        } catch (Exception ex) {
            try {
                rest.postForEntity(uploadServiceUrl + "/api/videos/" + id + "/status", Map.of("status","FAILED","failureReason", ex.getMessage()), Void.class);
            } catch (Exception e) {
                // swallow
            }
        }
    }

    private void run(List<String> cmd, File workingDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean finished = p.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("FFmpeg timed out");
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("FFmpeg failed with code " + p.exitValue());
        }
    }
}