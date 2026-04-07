package com.project.projectmanagementapplication.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectRequest {

    private String name;

    private String description;

    private List<String> tags = new ArrayList<>();

    private String category;

    /**
     * Optional on create: {@code KANBAN} or {@code SCRUM} (case-insensitive).
     * When null or blank, defaults to {@code KANBAN}.
     * Intentionally not applied on PATCH update — framework is immutable after creation.
     */
    private String framework;
}
