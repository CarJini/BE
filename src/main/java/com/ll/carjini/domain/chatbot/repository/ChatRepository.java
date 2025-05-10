package com.ll.carjini.domain.chatbot.repository;

import com.ll.carjini.domain.chatbot.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface ChatRepository extends MongoRepository<Chat, String> {
    Page<Chat> findByCarOwnerId(Long userId, Pageable pageable);

    void deleteByCarOwnerId(Long carOwnerId);
}
