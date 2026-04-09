package com.project.projectmanagementapplication.runner;

import com.project.projectmanagementapplication.service.ProjectMembershipService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class ProjectMembershipBackfillRunner implements ApplicationRunner {

    private final ProjectMembershipService membershipService;

    public ProjectMembershipBackfillRunner(ProjectMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Override
    public void run(ApplicationArguments args) {
        membershipService.backfillAllProjects();
    }
}
