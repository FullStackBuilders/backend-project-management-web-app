package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.enums.ISSUE_STATUS;
import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.model.BoardColumnSetting;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class BoardColumnSettingRepositoryTest {

    @Autowired
    private BoardColumnSettingRepository boardColumnSettingRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private Project project;

    @BeforeEach
    void setUp() {
        boardColumnSettingRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        User owner = new User();
        owner.setEmail("board-owner@example.com");
        owner.setFirstName("B");
        owner.setLastName("O");
        owner.setPassword("pw");
        owner = userRepository.save(owner);

        project = new Project();
        project.setName("BoardProj");
        project.setDescription("D");
        project.setCategory("C");
        project.setOwner(owner);
        project.setFramework(PROJECT_FRAMEWORK.KANBAN);
        project.getTeam().add(owner);
        project = projectRepository.save(project);
    }

    @Test
    void findByProject_Id_returnsSettings() {
        BoardColumnSetting row = new BoardColumnSetting();
        row.setProject(project);
        row.setStatus(ISSUE_STATUS.IN_PROGRESS);
        row.setWipLimit(3);
        boardColumnSettingRepository.save(row);

        List<BoardColumnSetting> list = boardColumnSettingRepository.findByProject_Id(project.getId());
        assertEquals(1, list.size());
        assertEquals(3, list.get(0).getWipLimit());
        assertEquals(ISSUE_STATUS.IN_PROGRESS, list.get(0).getStatus());
    }

    @Test
    void findByProject_IdAndStatus_whenPresent() {
        BoardColumnSetting row = new BoardColumnSetting();
        row.setProject(project);
        row.setStatus(ISSUE_STATUS.TO_DO);
        row.setWipLimit(5);
        boardColumnSettingRepository.save(row);

        Optional<BoardColumnSetting> found =
                boardColumnSettingRepository.findByProject_IdAndStatus(project.getId(), ISSUE_STATUS.TO_DO);
        assertTrue(found.isPresent());
        assertEquals(5, found.get().getWipLimit());
    }
}
