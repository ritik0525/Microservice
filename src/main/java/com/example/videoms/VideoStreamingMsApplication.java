package com.example.videoms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VideoStreamingMsApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoStreamingMsApplication.class, args);
    }
}