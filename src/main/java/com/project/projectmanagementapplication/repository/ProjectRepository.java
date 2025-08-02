package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.model.Project;
import com.project.projectmanagementapplication.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

//    List<Project> findByOwnerId(User user);

    @Query("SELECT p FROM Project p JOIN p.team t WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND t = :user")
    List<Project> searchProjectsByNameAndTeam(@Param("keyword") String keyword, @Param("user") User user);

    List<Project> findByNameContainingIgnoreCaseAndTeamContains(String name, User user);


//    @Query("SELECT p FROM Project p JOIN p.team t WHERE t = :user")
//    List<Project> findProjectByTeam(@Param("user") User user);

    List<Project> findByTeamContainingOrOwner(User user, User owner);



}
