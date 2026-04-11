package com.project.projectmanagementapplication.mapper;

import com.project.projectmanagementapplication.dto.SprintResponse;
import com.project.projectmanagementapplication.model.Sprint;
import org.springframework.stereotype.Component;

@Component
public class SprintMapper {

    public SprintResponse toResponse(Sprint sprint) {
        if (sprint == null) {
            return null;
        }
        return SprintResponse.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .status(sprint.getStatus())
                .projectId(sprint.getProject() != null ? sprint.getProject().getId() : null)
                .createdAt(sprint.getCreatedAt())
                .build();
    }
}
