package com.ll.carjini.domain.chatbot.repository;

import com.ll.carjini.domain.chatbot.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatRepository extends MongoRepository<Chat, String> {
    List<Chat> findTop3ByCarOwnerIdOrderByCreatedAtDesc(Long carOwnerId);
    Page<Chat> findByCarOwnerId(Long userId, Pageable pageable);
}
