package com.ll.carjini.domain.chatbot.controller;

import com.ll.carjini.domain.car.entity.Car;
import com.ll.carjini.domain.chatbot.dto.ChatbotRequest;
import com.ll.carjini.domain.chatbot.dto.ChatbotResponse;
import com.ll.carjini.domain.chatbot.entity.Chat;
import com.ll.carjini.domain.chatbot.service.ChatbotService;
import com.ll.carjini.domain.chatbot.service.InferenceService;
import com.ll.carjini.domain.chatbot.service.RedisChatService;
import com.ll.carjini.global.dto.GlobalResponse;
import com.ll.carjini.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
@Slf4j
@Tag(name = "챗봇 API", description = "내 차량별 챗봇과의 대화 API")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final RedisChatService redisChatService;
    private final InferenceService inferenceService;

//    @PostMapping("/{carOwnerId}")
//    @Operation(summary = "챗봇과 대화", description = "챗봇과의 대화를 처리합니다.")
//    public GlobalResponse<ChatbotResponse> sendChat(
//            @PathVariable Long carOwnerId,
//            @RequestBody ChatbotRequest chatBotRequest) {
//
//        ChatbotResponse chatBotResponse = chatbotService.processMessage(carOwnerId, chatBotRequest);
//        return GlobalResponse.success(chatBotResponse);
//    }

    @PostMapping("/{carOwnerId}")
    public GlobalResponse<ChatbotResponse> chat(
            @PathVariable Long carOwnerId,
            @RequestBody ChatbotRequest request) {
        try {
            // 사용자 메시지 저장
            log.info("Received chat request: {}", request);
            Chat userChat = chatbotService.saveUserMessageToRedis(carOwnerId, request);

            // 최근 대화 히스토리 가져오기
            List<Chat> history = redisChatService.getRecentChats(carOwnerId, 3);
            Long carId = chatbotService.getCarIdByOwnerId(carOwnerId);
            log.info("Retrieved chat history for car owner {}: {}", carOwnerId, history);
            // AI 답변 생성
            String answer = "";
            if (carId.equals(1L)) {
                answer = inferenceService.processStariaQuery(request.getMessage(), history);
            }else{
                answer = inferenceService.processGrandeurQuery(request.getMessage(), history);
            }
            log.info("Chatbot response created: {}", answer);

            // 봇 답변 저장
            Chat botChat = chatbotService.saveBotMessageToRedis(carOwnerId, answer);

            ChatbotResponse chat = ChatbotResponse.builder()
                    .sender("BOT")
                    .carOwnerId(carOwnerId)
                    .message(answer)
                    .createdAt(botChat.getCreatedAt())
                    .build();
            log.info("Chatbot response created: {}", answer);

            return GlobalResponse.success(chat);

        } catch (Exception e) {
            return GlobalResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
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


