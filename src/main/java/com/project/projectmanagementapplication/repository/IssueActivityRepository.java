package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.model.IssueActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueActivityRepository extends JpaRepository<IssueActivity, Long> {

    Page<IssueActivity> findByIssue_IdOrderByCreatedAtDesc(Long issueId, Pageable pageable);
}
