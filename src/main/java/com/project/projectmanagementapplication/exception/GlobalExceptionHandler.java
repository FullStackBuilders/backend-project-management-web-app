package com.project.projectmanagementapplication.exception;

import com.project.projectmanagementapplication.dto.InvitationConflictDetails;
import com.project.projectmanagementapplication.dto.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .status(HttpStatus.NOT_FOUND)
                .error("Resource Not Found")
                .message(ex.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .status(HttpStatus.BAD_REQUEST)
                .error("Bad Request")
                .message(ex.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, WebRequest request){
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .status(HttpStatus.UNAUTHORIZED)
                .error("Unauthorized")
                .message(ex.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request){
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .status(HttpStatus.UNAUTHORIZED)
                .error("Unauthorized")
                .message(ex.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }
    
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .statusCode(HttpStatus.CONFLICT.value())
                .status(HttpStatus.CONFLICT)
                .error("Conflict")
                .message(ex.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(InvitationAlreadySentException.class)
    public ResponseEntity<ErrorResponse> handleInvitationAlreadySent(InvitationAlreadySentException ex, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();

        // Create additional details for this specific conflict
        InvitationConflictDetails details = InvitationConflictDetails.builder()
                .email(ex.getEmail())
                .projectId(ex.getProjectId())
                .canResend(true)
                .build();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .statusCode(HttpStatus.CONFLICT.value())
                .status(HttpStatus.CONFLICT)
                .error("Invitation Already Sent")
                .message(ex.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InsightGenerationException.class)
    public ResponseEntity<ErrorResponse> handleInsightGeneration(InsightGenerationException ex, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        log.warn("Insight generation failed at {}: {}", path, ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_GATEWAY.value())
                .status(HttpStatus.BAD_GATEWAY)
                .error("Insight Generation Failed")
                .message("Unable to generate insights right now. Please try again later.")
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    @ExceptionHandler(InvitationExpiredException.class)
    public ResponseEntity<ErrorResponse> handleInvitationExpired(InvitationExpiredException ex, WebRequest request) {
        log.warn("Invitation expired at {}: {}", ((ServletWebRequest) request).getRequest().getRequestURI(), ex.getMessage());
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .statusCode(HttpStatus.GONE.value())
                .status(HttpStatus.GONE)
                .error("Invitation Expired")
                .message(ex.getMessage())
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.GONE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest request) {
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        log.error("Unhandled exception at {}: {}", path, ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .error("Internal Server Error")
                .message("Something went wrong. Please try again.")
                .path(path)
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
