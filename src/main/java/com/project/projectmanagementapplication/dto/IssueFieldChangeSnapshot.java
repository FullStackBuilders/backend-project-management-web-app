package com.project.projectmanagementapplication.dto;

/**
 * One detected field change for journaling (uses {@code field_name} string values).
 */
public record IssueFieldChangeSnapshot(String fieldName, String oldValue, String newValue) {}
