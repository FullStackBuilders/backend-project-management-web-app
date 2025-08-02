package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.IssueRequest;
import com.project.projectmanagementapplication.dto.IssueResponse;
import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.model.Issue;
import com.project.projectmanagementapplication.model.User;

import java.util.List;
import java.util.Optional;


public interface IssueService {
    Issue getIssueById(Long issueId) throws Exception;

    Response<List<IssueResponse>>  getIssueByProjectId(Long projectId) throws Exception;

    Response<IssueResponse> createIssue(Long projectId,IssueRequest issue, User user) throws Exception;

    Response<Long>  deleteIssue(Long issueId, Long userId) throws Exception;

    Response<IssueResponse>  addUserToIssue(Long issueId, Long userId) throws Exception;

    Response<IssueResponse> updateIssueStatus(Long issueId, String status, Long userId) throws Exception;

    Response<IssueResponse> updateIssue(Long issueId, IssueRequest issueRequest, Long userId) throws Exception;
}
