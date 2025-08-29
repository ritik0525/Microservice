package com.example.transcoder.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String VIDEO_UPLOADED_QUEUE = "video.uploaded";

    @Bean
    public Queue videoUploadedQueue() {
        return new Queue(VIDEO_UPLOADED_QUEUE, true);
    }
}