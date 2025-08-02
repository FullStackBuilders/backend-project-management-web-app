package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Comment;

import java.util.List;

public interface CommentService {
    Response<Comment> createComment(Long issueId, String commentText, Long userId) throws Exception;

    Response<Void> deleteComment(Long commentId, Long userId) throws Exception;

    Response<List<Comment>> getCommentsByIssueId(Long issueId) throws Exception;
}
