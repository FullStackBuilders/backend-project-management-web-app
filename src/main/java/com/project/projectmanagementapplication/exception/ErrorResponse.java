package com.project.projectmanagementapplication.exception;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Builder
@Data
public class ErrorResponse {
        private int statusCode;
        private HttpStatus status;
        private String error;
        private String message;
        private String path;
        private Object details;
        private String timestamp;
    }


