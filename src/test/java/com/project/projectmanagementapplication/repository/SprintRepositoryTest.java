package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.enums.PROJECT_FRAMEWORK;
import com.project.projectmanagementapplication.enums.SPRINT_STATUS;
import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.Sprint;
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
class SprintRepositoryTest {

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private Project project;

    @BeforeEach
    void setUp() {
        sprintRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        User owner = new User();
        owner.setEmail("sprint-owner@example.com");
        owner.setFirstName("S");
        owner.setLastName("O");
        owner.setPassword("pw");
        owner = userRepository.save(owner);

        project = new Project();
        project.setName("P");
        project.setDescription("D");
        project.setCategory("C");
        project.setOwner(owner);
        project.setFramework(PROJECT_FRAMEWORK.SCRUM);
        project.getTeam().add(owner);
        project = projectRepository.save(project);
    }

    @Test
    void findByProject_Id_returnsAllSprintsForProject() {
        Sprint a = new Sprint();
        a.setName("A");
        a.setStatus(SPRINT_STATUS.ACTIVE);
        a.setProject(project);
        sprintRepository.save(a);

        Sprint b = new Sprint();
        b.setName("B");
        b.setStatus(SPRINT_STATUS.INACTIVE);
        b.setProject(project);
        sprintRepository.save(b);

        List<Sprint> list = sprintRepository.findByProject_Id(project.getId());
        assertEquals(2, list.size());
    }

    @Test
    void findByIdAndProject_Id_whenPresent() {
        Sprint s = new Sprint();
        s.setName("One");
        s.setStatus(SPRINT_STATUS.ACTIVE);
        s.setProject(project);
        s = sprintRepository.save(s);

        Optional<Sprint> found = sprintRepository.findByIdAndProject_Id(s.getId(), project.getId());
        assertTrue(found.isPresent());
        assertEquals("One", found.get().getName());
    }

    @Test
    void findByIdAndProject_Id_whenWrongProject_empty() {
        User other = new User();
        other.setEmail("other@example.com");
        other.setFirstName("O");
        other.setLastName("T");
        other.setPassword("pw");
        other = userRepository.save(other);

        Project otherProject = new Project();
        otherProject.setName("P2");
        otherProject.setDescription("D");
        otherProject.setCategory("C");
        otherProject.setOwner(other);
        otherProject.setFramework(PROJECT_FRAMEWORK.SCRUM);
        otherProject.getTeam().add(other);
        otherProject = projectRepository.save(otherProject);

        Sprint s = new Sprint();
        s.setName("Solo");
        s.setStatus(SPRINT_STATUS.ACTIVE);
        s.setProject(project);
        s = sprintRepository.save(s);

        assertTrue(sprintRepository.findByIdAndProject_Id(s.getId(), otherProject.getId()).isEmpty());
    }
}
