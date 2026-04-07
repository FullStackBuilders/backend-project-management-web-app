package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.dto.SprintCreateRequest;
import com.project.projectmanagementapplication.dto.SprintResponse;
import com.project.projectmanagementapplication.dto.SprintStartRequest;
import com.project.projectmanagementapplication.model.User;

import java.util.List;

public interface SprintService {

    Response<List<SprintResponse>> listSprints(Long projectId, User caller);

    Response<SprintResponse> createSprint(Long projectId, SprintCreateRequest request, User owner);

    Response<SprintResponse> startSprint(Long projectId, Long sprintId, User owner, SprintStartRequest request);

    Response<SprintResponse> completeSprint(Long projectId, Long sprintId, User owner);
}
