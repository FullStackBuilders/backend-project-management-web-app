package com.project.projectmanagementapplication.dto;

import com.project.projectmanagementapplication.model.Chat;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.User;
import lombok.Data;


import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectRequest {

    private String name;

    private String description;

    private List<String> tags = new ArrayList<>();

    private String category;




}
