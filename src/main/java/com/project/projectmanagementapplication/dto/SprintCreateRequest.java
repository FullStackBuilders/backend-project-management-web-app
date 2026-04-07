package com.project.projectmanagementapplication.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SprintCreateRequest {

    private String name;

    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;
}
