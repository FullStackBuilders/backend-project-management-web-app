package com.project.projectmanagementapplication.util;

import com.project.projectmanagementapplication.dto.IssueTimelineItemDto;
import com.project.projectmanagementapplication.model.Comment;
import com.project.projectmanagementapplication.model.IssueActivity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges activity rows and comments into one reverse-chronological list.
 */
public final class IssueTimelineMerger {

    private IssueTimelineMerger() {}

    public static List<IssueTimelineItemDto> merge(
            List<IssueActivity> activitiesNewestFirst,
            List<Comment> commentsNewestFirst,
            int maxItems) {

        int i = 0;
        int j = 0;
        List<IssueTimelineItemDto> out = new ArrayList<>();

        while (out.size() < maxItems && (i < activitiesNewestFirst.size() || j < commentsNewestFirst.size())) {
            LocalDateTime nextAct = i < activitiesNewestFirst.size()
                    ? activitiesNewestFirst.get(i).getCreatedAt()
                    : LocalDateTime.MIN;
            LocalDateTime nextCom = j < commentsNewestFirst.size()
                    ? commentsNewestFirst.get(j).getCreatedDateTime()
                    : LocalDateTime.MIN;

            if (i >= activitiesNewestFirst.size()) {
                out.add(toCommentItem(commentsNewestFirst.get(j++)));
            } else if (j >= commentsNewestFirst.size()) {
                out.add(toActivityItem(activitiesNewestFirst.get(i++)));
            } else if (nextAct.isAfter(nextCom)) {
                out.add(toActivityItem(activitiesNewestFirst.get(i++)));
            } else {
                out.add(toCommentItem(commentsNewestFirst.get(j++)));
            }
        }

        return out;
    }

    private static IssueTimelineItemDto toActivityItem(IssueActivity a) {
        return IssueTimelineItemDto.builder()
                .kind("activity")
                .occurredAt(a.getCreatedAt())
                .activityId(a.getId())
                .activityType(a.getActivityType().name())
                .fieldName(a.getFieldName())
                .oldValue(a.getOldValue())
                .newValue(a.getNewValue())
                .actorUserId(a.getActor().getId())
                .actorName(fullName(a.getActor()))
                .build();
    }

    private static IssueTimelineItemDto toCommentItem(Comment c) {
        return IssueTimelineItemDto.builder()
                .kind("comment")
                .occurredAt(c.getCreatedDateTime())
                .commentId(c.getId())
                .content(c.getContent())
                .commentAuthorUserId(c.getUser().getId())
                .commentAuthorName(fullName(c.getUser()))
                .build();
    }

    private static String fullName(com.project.projectmanagementapplication.model.User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
