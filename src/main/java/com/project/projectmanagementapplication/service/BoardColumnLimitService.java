package com.project.projectmanagementapplication.service;

import com.project.projectmanagementapplication.dto.BoardColumnLimitResponse;
import com.project.projectmanagementapplication.dto.UpdateBoardColumnLimitRequest;
import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.exception.BadRequestException;
import com.project.projectmanagementapplication.exception.UnauthorizedException;
import com.project.projectmanagementapplication.model.BoardColumnSetting;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.repository.BoardColumnSettingRepository;
import com.project.projectmanagementapplication.repository.IssueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BoardColumnLimitService {

    private final ProjectService projectService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final BoardColumnSettingRepository boardColumnSettingRepository;
    private final IssueRepository issueRepository;

    public BoardColumnLimitService(
            ProjectService projectService,
            ProjectAuthorizationService projectAuthorizationService,
            BoardColumnSettingRepository boardColumnSettingRepository,
            IssueRepository issueRepository) {
        this.projectService = projectService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.boardColumnSettingRepository = boardColumnSettingRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public List<BoardColumnLimitResponse> getColumnLimits(Long projectId, User caller) {
        Project project = projectService.getProjectByIdForUser(projectId, caller).getData();

        Map<ISSUE_STATUS, Integer> limitsByStatus =
                boardColumnSettingRepository.findByProject_Id(projectId).stream()
                        .collect(
                                Collectors.toMap(
                                        BoardColumnSetting::getStatus,
                                        BoardColumnSetting::getWipLimit,
                                        (left, right) -> left,
                                        () -> new EnumMap<>(ISSUE_STATUS.class)));

        return List.of(ISSUE_STATUS.TO_DO, ISSUE_STATUS.IN_PROGRESS, ISSUE_STATUS.DONE).stream()
                .map(
                        status -> {
                            long currentCount =
                                    issueRepository.countByProjectIdAndStatus(project.getId(), status);
                            Integer limit = limitsByStatus.get(status);
                            boolean exceeded = limit != null && currentCount > limit;
                            return BoardColumnLimitResponse.builder()
                                    .status(status.name())
                                    .wipLimit(limit)
                                    .currentCount(currentCount)
                                    .exceeded(exceeded)
                                    .build();
                        })
                .toList();
    }

    @Transactional
    public BoardColumnLimitResponse updateColumnLimit(
            Long projectId, String statusRaw, UpdateBoardColumnLimitRequest body, User caller) {
        if (!projectAuthorizationService.canManageProjectSettings(projectId, caller)) {
            throw new UnauthorizedException(
                    "Only project owners and admins can update board WIP limits");
        }

        Project project = projectService.getProjectById(projectId).getData();
        ISSUE_STATUS status = parseStatus(statusRaw);

        Integer nextLimit = body == null ? null : body.getWipLimit();
        if (nextLimit != null && nextLimit < 1) {
            throw new BadRequestException("WIP limit must be a whole number greater than 0");
        }

        BoardColumnSetting setting =
                boardColumnSettingRepository
                        .findByProject_IdAndStatus(projectId, status)
                        .orElseGet(
                                () -> {
                                    BoardColumnSetting created = new BoardColumnSetting();
                                    created.setProject(project);
                                    created.setStatus(status);
                                    return created;
                                });

        setting.setWipLimit(nextLimit);
        boardColumnSettingRepository.save(setting);

        long currentCount = issueRepository.countByProjectIdAndStatus(projectId, status);
        boolean exceeded = nextLimit != null && currentCount > nextLimit;

        return BoardColumnLimitResponse.builder()
                .status(status.name())
                .wipLimit(nextLimit)
                .currentCount(currentCount)
                .exceeded(exceeded)
                .build();
    }

    @Transactional
    public BoardColumnLimitResponse clearColumnLimit(Long projectId, String statusRaw, User caller) {
        UpdateBoardColumnLimitRequest body = new UpdateBoardColumnLimitRequest();
        body.setWipLimit(null);
        return updateColumnLimit(projectId, statusRaw, body, caller);
    }

    private ISSUE_STATUS parseStatus(String raw) {
        try {
            return ISSUE_STATUS.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + raw);
        }
    }
}
