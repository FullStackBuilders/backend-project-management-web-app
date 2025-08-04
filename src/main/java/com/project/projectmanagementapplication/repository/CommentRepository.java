package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

 List<Comment> findByIssueId(Long issueId);

    List<Comment> findByIssueIdOrderByCreatedDateTimeAsc(Long issueId);
}
