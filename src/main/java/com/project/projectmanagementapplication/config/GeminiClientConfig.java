package com.project.projectmanagementapplication.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;


@Configuration
public class GeminiClientConfig {

    @Bean
    public Client geminiClient() {
        return new Client();
    }
}
