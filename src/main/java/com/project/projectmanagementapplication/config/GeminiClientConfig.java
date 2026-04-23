package com.project.projectmanagementapplication.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;


@Configuration
@ConditionalOnProperty(prefix = "gemini", name = "enabled", havingValue = "true")
public class GeminiClientConfig {

    @Bean
    public Client geminiClient() {
        return new Client();
    }
}
