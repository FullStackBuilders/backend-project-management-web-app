package com.project.projectmanagementapplication.exception;

/** Thrown when the Gemini metrics insights call fails or returns unusable output. */
public class InsightGenerationException extends RuntimeException {

    public InsightGenerationException(String message) {
        super(message);
    }

    public InsightGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
