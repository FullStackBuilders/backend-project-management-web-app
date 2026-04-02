package com.project.projectmanagementapplication.mapper;

import com.project.projectmanagementapplication.dto.CommentResponse;
import com.project.projectmanagementapplication.model.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getFirstName() + " " + comment.getUser().getLastName())
                .createdDateTime(comment.getCreatedDateTime())
                .build();
    }
}
