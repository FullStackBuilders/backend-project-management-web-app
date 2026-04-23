package com.project.projectmanagementapplication.controller;


import com.project.projectmanagementapplication.dto.ProjectMembershipMeResponse;
import com.project.projectmanagementapplication.dto.ProjectRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.dto.BoardColumnLimitResponse;
import com.project.projectmanagementapplication.dto.UpdateBoardColumnLimitRequest;
import com.project.projectmanagementapplication.dto.UpdateProjectMemberRoleRequest;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.ChatService;
import com.project.projectmanagementapplication.service.InvitationService;
import com.project.projectmanagementapplication.service.ProjectMembershipService;
import com.project.projectmanagementapplication.service.ProjectService;
import com.project.projectmanagementapplication.service.UserService;
import com.project.projectmanagementapplication.service.BoardColumnLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ChatService chatService;
    private final UserService userService;
    private final InvitationService invitationService;
    private final BoardColumnLimitService boardColumnLimitService;

    private final ProjectMembershipService projectMembershipService;

    @Autowired
    public ProjectController(
            ProjectService projectService,
            ChatService chatService,
            UserService userService,
            InvitationService invitationService,
            ProjectMembershipService projectMembershipService,
            BoardColumnLimitService boardColumnLimitService) {
        this.projectService = projectService;
        this.chatService = chatService;
        this.userService = userService;
        this.invitationService = invitationService;
        this.projectMembershipService = projectMembershipService;
        this.boardColumnLimitService = boardColumnLimitService;
    }

     @GetMapping
     public ResponseEntity<List<Project>> getProjects(@RequestParam(required = false) String category,
                                                      @RequestParam(required = false) String tag)  {

         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
         String username = authentication.getName();
         User user = userService.findByUsername(username);
         Response<List<Project>> response = projectService.getAllProjectForUser(user, category, tag);
         return ResponseEntity.ok(response.getData());


     }

    @GetMapping("/{projectId}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long projectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        Response<Project> response = projectService.getProjectByIdForUser(projectId, user);
        return ResponseEntity.status(response.getStatus()).body(response.getData());


    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody ProjectRequest projectRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<Project> response = projectService.createProject(projectRequest, user);
        return ResponseEntity.status(response.getStatus()).body(response.getData());
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<Project> updateProject(@PathVariable Long projectId,
                                                        @RequestBody ProjectRequest projectRequest)  {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User currentUser = userService.findByUsername(username);
        Response<Project> response = projectService.updateProject(projectRequest, projectId, currentUser);
        return ResponseEntity.ok(response.getData());
    }

    @GetMapping("/{projectId}/membership/me")
    public ResponseEntity<ProjectMembershipMeResponse> getMyProjectMembership(@PathVariable Long projectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        projectService.getProjectById(projectId);
        var role = projectMembershipService.getRole(projectId, user.getId());
        return ResponseEntity.ok(
                ProjectMembershipMeResponse.builder().role(role.name()).build());
    }

    @PatchMapping("/{projectId}/members/m/{memberUserId}/role")
    public ResponseEntity<ProjectMembershipMeResponse> updateProjectMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberUserId,
            @RequestBody UpdateProjectMemberRoleRequest body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User caller = userService.findByUsername(authentication.getName());
        Project project = projectService.getProjectById(projectId).getData();
        if (body == null || body.getRole() == null) {
            throw new BadRequestException("role is required");
        }
        var updated =
                projectMembershipService.updateMemberRole(project, caller, memberUserId, body.getRole());
        return ResponseEntity.ok(ProjectMembershipMeResponse.builder().role(updated.name()).build());
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Response<Void>> deleteProject(@PathVariable Long projectId)  {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<Void> response = projectService.deleteProject(projectId, user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}/board-columns/limits")
    public ResponseEntity<List<BoardColumnLimitResponse>> getBoardColumnLimits(@PathVariable Long projectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(boardColumnLimitService.getColumnLimits(projectId, user));
    }

    @PutMapping("/{projectId}/board-columns/{status}/limit")
    public ResponseEntity<BoardColumnLimitResponse> updateBoardColumnLimit(
            @PathVariable Long projectId,
            @PathVariable String status,
            @RequestBody(required = false) UpdateBoardColumnLimitRequest body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(boardColumnLimitService.updateColumnLimit(projectId, status, body, user));
    }

    @DeleteMapping("/{projectId}/board-columns/{status}/limit")
    public ResponseEntity<BoardColumnLimitResponse> clearBoardColumnLimit(
            @PathVariable Long projectId,
            @PathVariable String status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(boardColumnLimitService.clearColumnLimit(projectId, status, user));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Project>> searchProjects(@RequestParam String keyword) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<List<Project>> response = projectService.searchProjects(keyword, user);
        return ResponseEntity.ok(response.getData());
    }

    @GetMapping("/{projectId}/chat")
    public ResponseEntity<Chat> getChatByProjectId(@PathVariable Long projectId)  {

        Response<Chat> response = chatService.getChatByProjectId(projectId);
        return ResponseEntity.status(response.getStatus()).body(response.getData());
    }


//    @PostMapping("/invite")
//    public ResponseEntity<Response<Void>> inviteUserToProject(@RequestBody EmailInviteRequest emailInviteRequest) throws Exception {
//
//    Response<Void> response  = invitationService.sendInvitation(emailInviteRequest.getEmail(), emailInviteRequest.getProjectId());
//    return ResponseEntity.ok(response);
//    }
//
//    @GetMapping("/accept-invitation")
//    public ResponseEntity<Response<Invitation>> acceptInvitation(@RequestParam String token) throws Exception {
//
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//        User user = userService.findByUsername(username);
//        Response<Invitation> response =  invitationService.acceptInvitation(token, user.getId());
//        projectService.addUserToProject(response.getData().getProjectId(), user.getId());
//        return ResponseEntity.status(response.getStatus()).body(response);
//    }
}
