package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.CommentRequest;
import com.project.projectmanagementapplication.dto.CommentResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.model.Comment;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.CommentRepository;
import com.project.projectmanagementapplication.repository.IssueRepository;
import com.project.projectmanagementapplication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final IssueService issueService;
    private final UserService userService;

    @Autowired
    public CommentServiceImpl(CommentRepository commentRepository, IssueService issueService, IssueService issueService1, UserService userService) {
        this.commentRepository = commentRepository;
        this.issueService = issueService1;
        this.userService = userService;
    }

    @Override
    public Response<Comment> createComment(Long issueId, String content, Long userId) throws Exception {
        Issue issue  = issueService.getIssueById(issueId);
        User user = userService.findByUserId(userId);

        Comment comment = new Comment();
        comment.setIssue(issue);
        comment.setUser(user);
        comment.setContent(content);
        comment.setCreatedDateTime(LocalDateTime.now());


        Comment savedComment = commentRepository.save(comment);
        issue.getComments().add(savedComment);
        return Response.<Comment>builder()
                .status(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .message("Comment created successfully")
                .data(savedComment)
                .timestamp(LocalDateTime.now().toString())
                .build();

    }

    @Override
    public Response<Void> deleteComment(Long commentId, Long userId) throws Exception {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new Exception("Comment not found with ID: " + commentId));
        User user = userService.findByUserId(userId);

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new Exception("User does not have permission to delete this comment");
        }
        commentRepository.delete(comment);

        return Response.<Void>builder()
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("Comment deleted successfully")
                .timestamp(LocalDateTime.now().toString())
                .build();


    }

    @Override
    public Response<List<CommentResponse>> getCommentsByIssueId(Long issueId) throws Exception {
        // Verify issue exists
        Issue issue = issueService.getIssueById(issueId);

        List<Comment> comments = commentRepository.findByIssueIdOrderByCreatedDateTimeAsc(issueId);

        List<CommentResponse> commentResponses = comments.stream()
                .map(comment -> CommentResponse.builder()
                        .id(comment.getId())
                        .content(comment.getContent())
                        .createdDateTime(comment.getCreatedDateTime())
                        .userId(comment.getUser().getId())
                        .userName(comment.getUser().getFirstName() + " " + comment.getUser().getLastName())
                        .issueId(comment.getIssue().getId())
                        .build())
                .toList();

        String message = comments.isEmpty() ?
                "No comments found for this issue" :
                "Comments retrieved successfully";

        return Response.<List<CommentResponse>>builder()
                .data(commentResponses)
                .message(message)
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Response<CommentResponse> addComment(Long issueId, CommentRequest commentRequest, User user) throws Exception {
        Issue issue = issueService.getIssueById(issueId);
        Project project = issue.getProject();

        // Check if user can comment (assignee, issue creator, or project owner)
        boolean canComment = false;

        if (issue.getAssignee() != null && issue.getAssignee().getId().equals(user.getId())) {
            canComment = true;
        } else if (issue.getCreatedBy().getId().equals(user.getId())) {
            canComment = true;
        } else if (project.getOwner().getId().equals(user.getId())) {
            canComment = true;
        }

        if (!canComment) {
            throw new UnauthorizedException("Only the assignee, issue creator, or project owner can comment on this issue");
        }

        Comment comment = new Comment();
        comment.setContent(commentRequest.getContent());
        comment.setCreatedDateTime(LocalDateTime.now());
        comment.setUser(user);
        comment.setIssue(issue);

        Comment savedComment = commentRepository.save(comment);

        CommentResponse commentResponse = CommentResponse.builder()
                .id(savedComment.getId())
                .content(savedComment.getContent())
                .createdDateTime(savedComment.getCreatedDateTime())
                .userId(savedComment.getUser().getId())
                .userName(savedComment.getUser().getFirstName() + " " + savedComment.getUser().getLastName())
                .issueId(savedComment.getIssue().getId())
                .build();

        return Response.<CommentResponse>builder()
                .data(commentResponse)
                .message("Comment added successfully")
                .status(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }




}
