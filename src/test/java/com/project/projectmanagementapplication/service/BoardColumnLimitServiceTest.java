package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.Response;
import com.project.projectmanagementapplication.dto.UpdateBoardColumnLimitRequest;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.model.BoardColumnSetting;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.BoardColumnSettingRepository;
import com.project.projectmanagementapplication.repository.IssueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardColumnLimitServiceTest {

    @Mock private ProjectService projectService;
    @Mock private ProjectAuthorizationService projectAuthorizationService;
    @Mock private BoardColumnSettingRepository boardColumnSettingRepository;
    @Mock private IssueRepository issueRepository;

    @InjectMocks private BoardColumnLimitService boardColumnLimitService;

    @Test
    void getColumnLimits_returnsConfiguredLimitsWithExceededFlag() {
        Long projectId = 9L;
        User caller = new User();
        caller.setId(10L);
        Project project = new Project();
        project.setId(projectId);

        BoardColumnSetting inProgress = new BoardColumnSetting();
        inProgress.setProject(project);
        inProgress.setStatus(ISSUE_STATUS.IN_PROGRESS);
        inProgress.setWipLimit(2);

        when(projectService.getProjectByIdForUser(projectId, caller))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(boardColumnSettingRepository.findByProject_Id(projectId)).thenReturn(List.of(inProgress));
        when(issueRepository.countByProjectIdAndStatus(projectId, ISSUE_STATUS.TO_DO)).thenReturn(1L);
        when(issueRepository.countByProjectIdAndStatus(projectId, ISSUE_STATUS.IN_PROGRESS)).thenReturn(3L);
        when(issueRepository.countByProjectIdAndStatus(projectId, ISSUE_STATUS.DONE)).thenReturn(4L);

        var rows = boardColumnLimitService.getColumnLimits(projectId, caller);

        assertEquals(3, rows.size());
        assertEquals("IN_PROGRESS", rows.get(1).getStatus());
        assertEquals(2, rows.get(1).getWipLimit());
        assertEquals(3L, rows.get(1).getCurrentCount());
        assertEquals(true, rows.get(1).isExceeded());
    }

    @Test
    void updateColumnLimit_rejectsNonPositiveLimit() {
        Long projectId = 9L;
        User caller = new User();
        caller.setId(10L);

        when(projectAuthorizationService.canManageProjectSettings(projectId, caller)).thenReturn(true);
        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(new Project()).build());

        UpdateBoardColumnLimitRequest body = new UpdateBoardColumnLimitRequest();
        body.setWipLimit(0);

        assertThrows(
                BadRequestException.class,
                () -> boardColumnLimitService.updateColumnLimit(projectId, "IN_PROGRESS", body, caller));
    }

    @Test
    void updateColumnLimit_createsOrUpdatesSetting() {
        Long projectId = 9L;
        User caller = new User();
        caller.setId(10L);
        Project project = new Project();
        project.setId(projectId);

        when(projectAuthorizationService.canManageProjectSettings(projectId, caller)).thenReturn(true);
        when(projectService.getProjectById(projectId))
                .thenReturn(Response.<Project>builder().data(project).build());
        when(boardColumnSettingRepository.findByProject_IdAndStatus(projectId, ISSUE_STATUS.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(issueRepository.countByProjectIdAndStatus(projectId, ISSUE_STATUS.IN_PROGRESS)).thenReturn(5L);

        UpdateBoardColumnLimitRequest body = new UpdateBoardColumnLimitRequest();
        body.setWipLimit(6);

        var row = boardColumnLimitService.updateColumnLimit(projectId, "IN_PROGRESS", body, caller);

        assertEquals("IN_PROGRESS", row.getStatus());
        assertEquals(6, row.getWipLimit());
        assertEquals(5L, row.getCurrentCount());
        assertEquals(false, row.isExceeded());
        verify(boardColumnSettingRepository).save(any(BoardColumnSetting.class));
        verify(issueRepository).countByProjectIdAndStatus(eq(projectId), eq(ISSUE_STATUS.IN_PROGRESS));
    }
}
