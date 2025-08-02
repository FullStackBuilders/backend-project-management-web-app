package com.project.projectmanagementapplication.repository;


import com.project.projectmanagementapplication.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    public List<Issue> findByProjectId(Long projectId);

}
