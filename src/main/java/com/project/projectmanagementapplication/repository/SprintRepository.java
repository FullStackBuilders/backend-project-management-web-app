package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findByProject_Id(Long projectId);

    Optional<Sprint> findByIdAndProject_Id(Long id, Long projectId);
}
