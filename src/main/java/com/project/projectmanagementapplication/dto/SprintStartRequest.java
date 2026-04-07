package com.project.projectmanagementapplication.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SprintStartRequest {

    private LocalDate startDate;

    private LocalDate endDate;
}
