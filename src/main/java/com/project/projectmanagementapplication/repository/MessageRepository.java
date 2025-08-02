package com.project.projectmanagementapplication.repository;

import com.project.projectmanagementapplication.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<ChatMessage,Long> {

    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(Long chatId);
}
