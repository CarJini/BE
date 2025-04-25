package com.ll.carjini.domain.chatbot.service;

import com.ll.carjini.domain.chatbot.dto.ChatbotRequest;
import com.ll.carjini.domain.chatbot.dto.ChatbotResponse;
import com.ll.carjini.domain.chatbot.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ChatbotService {

    ChatbotResponse processMessage(Long carOwnerId, ChatbotRequest request);
    Page<Chat> getChatHistoryPaginated(Long carOwnerId, Pageable pageable);

}