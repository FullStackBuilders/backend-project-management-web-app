package com.project.projectmanagementapplication.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ChatMessage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String content;

    @ManyToOne
    private Chat chat;

    @ManyToOne
    private User sender;


}
