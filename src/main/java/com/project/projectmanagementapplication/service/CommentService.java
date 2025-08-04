package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.CommentRequest;
import com.project.projectmanagementapplication.dto.CommentResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Comment;
import com.project.projectmanagementapplication.model.User;

import java.util.List;

public interface CommentService {
    Response<Comment> createComment(Long issueId, String commentText, Long userId) throws Exception;

    Response<Void> deleteComment(Long commentId, Long userId) throws Exception;

    Response<List<CommentResponse>> getCommentsByIssueId(Long issueId) throws Exception;

    Response<CommentResponse> addComment(Long issueId, CommentRequest commentRequest, User user) throws Exception;
}
