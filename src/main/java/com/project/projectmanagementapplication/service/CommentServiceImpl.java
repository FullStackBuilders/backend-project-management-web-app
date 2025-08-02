package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Comment;
import com.project.projectmanagementapplication.model.Issue;
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
    public Response<List<Comment>> getCommentsByIssueId(Long issueId) throws Exception {
        List<Comment> comments = commentRepository.findByIssueId(issueId);
        return Response.<List<Comment>>builder()
                .data(comments)
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .message("Comments retrieved successfully")
                .timestamp(LocalDateTime.now().toString())
                .build();

    }




}
