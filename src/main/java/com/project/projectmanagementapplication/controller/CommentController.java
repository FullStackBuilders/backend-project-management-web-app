package com.project.projectmanagementapplication.controller;

import com.project.projectmanagementapplication.dto.CommentRequest;
import com.project.projectmanagementapplication.dto.CommentResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Comment;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.CommentService;
import com.project.projectmanagementapplication.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;



    @Autowired
    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @GetMapping("/issue/{issueId}")
    public ResponseEntity<Response<List<CommentResponse>>> getCommentsByIssueId(@PathVariable Long issueId) throws Exception {
        Response<List<CommentResponse>> response = commentService.getCommentsByIssueId(issueId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/issue/{issueId}")
    public ResponseEntity<Response<CommentResponse>> addComment(
            @PathVariable Long issueId,
            @RequestBody CommentRequest commentRequest) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        Response<CommentResponse> response = commentService.addComment(issueId, commentRequest, user);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/{issueId}")
    public ResponseEntity<Response<Comment>> createComment(@PathVariable Long issueId, @RequestBody CommentRequest commentRequest) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<Comment> response = commentService.createComment(issueId, commentRequest.getContent(), user.getId());
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Response<Void>> deleteComment(@PathVariable Long commentId
                                              ) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<Void> response = commentService.deleteComment(commentId, user.getId());
        return ResponseEntity.status(response.getStatus()).body(response);

    }



}
