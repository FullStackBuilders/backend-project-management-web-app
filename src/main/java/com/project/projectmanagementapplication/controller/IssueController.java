package com.project.projectmanagementapplication.controller;

import com.project.projectmanagementapplication.dto.IssueRequest;
import com.project.projectmanagementapplication.dto.IssueResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.IssueService;
import com.project.projectmanagementapplication.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;
    private final UserService userService;

    public IssueController(IssueService issueService, UserService userService) {
        this.issueService = issueService;
        this.userService = userService;
    }

    @GetMapping("/{issueId}")
    public ResponseEntity<Issue> getIssueById(@PathVariable Long issueId) throws Exception {
        Issue issue = issueService.getIssueById(issueId);
        return ResponseEntity.ok(issue);

    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Response<List<IssueResponse>>>  getIssuesByProjectId(@PathVariable Long projectId) throws Exception {
        Response<List<IssueResponse>>  response = issueService.getIssueByProjectId(projectId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{projectId}")
    public ResponseEntity<Response<IssueResponse>> createIssue(@PathVariable Long projectId, @RequestBody IssueRequest issueRequest) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<IssueResponse> response = issueService.createIssue(projectId,issueRequest, user);
        return ResponseEntity.status(response.getStatus()).body(response);

    }

    @DeleteMapping("/{issueId}")
    public ResponseEntity<Response<Long>> deleteIssue(@PathVariable Long issueId) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<Long> response = issueService.deleteIssue(issueId, user.getId());
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{issueId}")
    public ResponseEntity<Response<IssueResponse>> updateIssue(
            @PathVariable Long issueId,
            @RequestBody IssueRequest issueRequest) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<IssueResponse> response = issueService.updateIssue(issueId, issueRequest, user.getId());
        return ResponseEntity.status(response.getStatus()).body(response);
    }


    @PutMapping("/{issueId}/assignee/{userId}")
    public ResponseEntity<Response<IssueResponse>> addUserToIssue(@PathVariable Long issueId,
                                                                  @PathVariable Long userId) throws Exception {

        Response<IssueResponse> response = issueService.addUserToIssue(issueId, userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PutMapping("/{issueId}/status/{status}")
    public ResponseEntity<Response<IssueResponse>> updateIssueStatus(@PathVariable Long issueId,
                                                                     @PathVariable String status) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<IssueResponse> response = issueService.updateIssueStatus(issueId, status, user.getId());
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}