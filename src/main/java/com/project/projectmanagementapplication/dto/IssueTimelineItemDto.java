package com.project.projectmanagementapplication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Unified timeline row for Issue Detail: {@code kind} is {@code activity} or {@code comment}.
 */
@Data
@Builder
@JsonInclude(NON_NULL)
public class IssueTimelineItemDto {
    private String kind;
    private LocalDateTime occurredAt;

    private Long activityId;
    private String activityType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long actorUserId;
    private String actorName;

    private Long commentId;
    private String content;
    private Long commentAuthorUserId;
    private String commentAuthorName;
}
