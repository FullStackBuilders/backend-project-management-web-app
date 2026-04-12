package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.ai.AiMetricsContextPayload;
import com.project.projectmanagementapplication.dto.ai.MetricsContextRequest;
import com.project.projectmanagementapplication.model.User;

public interface AiMetricsContextService {

    AiMetricsContextPayload buildContext(MetricsContextRequest request, User caller);
}
