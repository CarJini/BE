package com.ll.carjini.domain.chatbot.controller;

import com.ll.carjini.domain.chatbot.dto.ChatbotRequest;
import com.ll.carjini.domain.chatbot.dto.ChatbotResponse;
import com.ll.carjini.domain.chatbot.entity.Chat;
import com.ll.carjini.domain.chatbot.service.ChatbotService;
import com.ll.carjini.global.dto.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.connect.model.ChatMessage;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @MessageMapping("/send")
    public GlobalResponse<ChatbotResponse> sendChat(
            @RequestBody ChatbotRequest chatBotRequest) {

        ChatbotResponse chatBotResponse = chatbotService.processMessage(chatBotRequest);
        return GlobalResponse.success(chatBotResponse);
    }

    @GetMapping("/history/{carOwnerId}")
    public GlobalResponse<Page<Chat>> getChatHistory(
            @PathVariable Long carOwnerId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Chat> chatHistory = chatbotService.getChatHistoryPaginated(carOwnerId, pageable);
        return GlobalResponse.success(chatHistory);
    }

}
