package com.project.projectmanagementapplication.controller;


import com.project.projectmanagementapplication.dto.EmailInviteRequest;
import com.project.projectmanagementapplication.dto.ProjectRequest;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.Invitation;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.ChatService;
import com.project.projectmanagementapplication.service.InvitationService;
import com.project.projectmanagementapplication.service.ProjectService;
import com.project.projectmanagementapplication.service.UserService;
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




    @Autowired
    public ProjectController(ProjectService projectService, ChatService chatService, UserService userService, InvitationService invitationService) {
        this.projectService = projectService;
        this.chatService = chatService;
        this.userService = userService;
        this.invitationService = invitationService;
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

        Response<Project> response = projectService.getProjectById(projectId);
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

        Response<Project> response = projectService.updateProject(projectRequest, projectId);
        return ResponseEntity.ok(response.getData());
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Response<Void>> deleteProject(@PathVariable Long projectId)  {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<Void> response = projectService.deleteProject(projectId, user.getId());
        return ResponseEntity.ok(response);
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


    @PostMapping("/invite")
    public ResponseEntity<Response<Void>> inviteUserToProject(@RequestBody EmailInviteRequest emailInviteRequest) throws Exception {

    Response<Void> response  = invitationService.sendInvitation(emailInviteRequest.getEmail(), emailInviteRequest.getProjectId());
    return ResponseEntity.ok(response);
    }

    @GetMapping("/accept-invitation")
    public ResponseEntity<Response<Invitation>> acceptInvitation(@RequestParam String token) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Response<Invitation> response =  invitationService.acceptInvitation(token, user.getId());
        projectService.addUserToProject(response.getData().getProjectId(), user.getId());
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
