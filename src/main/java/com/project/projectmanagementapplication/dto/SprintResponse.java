package com.project.projectmanagementapplication.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SprintResponse {

    private Long id;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private SPRINT_STATUS status;
    private Long projectId;

    /** Creation time for tie-breaking sprint ordering on the client (ISO-like string for all Jackson versions). */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
