package com.project.projectmanagementapplication.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectDetailsResponse {
    private String projectName;
    private String projectDescription;
    private String projectCategory;
    private String ownerName;
    private int teamSize;
    private String token;
}
