package com.project.projectmanagementapplication.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IssueTimelineData {
    /** Merged activity + comment events, newest first. */
    private List<IssueTimelineItemDto> items;
    /** Effective limit applied (may truncate merged result). */
    private int limit;
}
