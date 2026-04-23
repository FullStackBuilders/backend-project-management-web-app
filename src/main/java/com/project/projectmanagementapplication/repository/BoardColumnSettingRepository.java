package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.model.BoardColumnSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardColumnSettingRepository extends JpaRepository<BoardColumnSetting, Long> {
    List<BoardColumnSetting> findByProject_Id(Long projectId);

    Optional<BoardColumnSetting> findByProject_IdAndStatus(Long projectId, ISSUE_STATUS status);
}
