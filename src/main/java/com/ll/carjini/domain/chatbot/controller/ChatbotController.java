package com.ll.carjini.domain.chatbot.controller;

import com.ll.carjini.domain.chatbot.dto.ChatbotRequest;
import com.ll.carjini.domain.chatbot.dto.ChatbotResponse;
import com.ll.carjini.domain.chatbot.entity.Chat;
import com.ll.carjini.domain.chatbot.service.ChatbotService;
import com.ll.carjini.global.dto.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
@Tag(name = "챗봇 API", description = "내 차량별 챗봇과의 대화 API")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/{carOwnerId}")
    @Operation(summary = "챗봇과 대화", description = "챗봇과의 대화를 처리합니다.")
    public GlobalResponse<ChatbotResponse> sendChat(
            @PathVariable Long carOwnerId,
            @RequestBody ChatbotRequest chatBotRequest) {

        ChatbotResponse chatBotResponse = chatbotService.processMessage(carOwnerId, chatBotRequest);
        return GlobalResponse.success(chatBotResponse);
    }

    @GetMapping("/history/{carOwnerId}")
    @Operation(summary = "챗봇 대화 내역 조회", description = "챗봇과의 대화 내역을 조회합니다.")
    public GlobalResponse<Page<Chat>> getChatHistory(
            @PathVariable Long carOwnerId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Chat> chatHistory = chatbotService.getChatHistoryPaginated(carOwnerId, pageable);
        return GlobalResponse.success(chatHistory);
    }

}
