package com.project.projectmanagementapplication.dto;

import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

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
}
