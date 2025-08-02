package com.project.projectmanagementapplication.model;

import com.project.projectmanagementapplication.enums.PLAN_TYPE;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDate subscriptionStartDate;

    private LocalDate subscriptionEndDate;

    private PLAN_TYPE planType;

    private boolean isValid;

    @OneToOne
    private User user;
}
