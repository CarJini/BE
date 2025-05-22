package com.ll.carjini.domain.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ll.carjini.domain.chatbot.dto.ChatbotRequest;
import com.ll.carjini.domain.chatbot.dto.ChatbotResponse;
import com.ll.carjini.domain.chatbot.entity.Chat;
import com.ll.carjini.domain.chatbot.entity.SenderType;
import com.ll.carjini.domain.chatbot.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceHFImpl implements ChatbotService {

    private final RestTemplate restTemplate;
    private final ChatRepository chatRepository;
    private final RedisChatService redisChatService;


    @Override
    public ChatbotResponse processMessage(Long carOwnerId, ChatbotRequest request) {
        saveUserMessageToRedis(carOwnerId, request);

        String botResponseText = callFastApi( carOwnerId, request.getMessage());

        Chat botMessage = saveBotMessageToRedis(carOwnerId, botResponseText);

        return ChatbotResponse.builder()
                .carOwnerId(carOwnerId)
                .message(botResponseText)
                .createdAt(botMessage.getCreatedAt())
                .build();
    }

    @Override
    public Page<Chat> getChatHistoryPaginated(Long carOwnerId, Pageable pageable) {
        log.info("Fetching paginated chatbot history for user: {}, pageable: {}", carOwnerId, pageable);

        endChatSession(carOwnerId);
        return chatRepository.findByCarOwnerId(carOwnerId, pageable);
    }

    private String callFastApi(Long carOwnerId, String userMessage) {
        List<Chat> recentChats = redisChatService.getRecentChats(carOwnerId, 3);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ArrayNode historyArray = objectMapper.createArrayNode();
            for (Chat chat : recentChats) {
                ObjectNode messageNode = objectMapper.createObjectNode();
                messageNode.put("role", chat.getSender() == SenderType.USER ? "user" : "assistant");
                messageNode.put("content", chat.getMessage());
                historyArray.add(messageNode);
            }

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("query", userMessage);
            requestBody.set("history", historyArray);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    "http://localhost:8000/answer",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(responseEntity.getBody());
            return root.path("answer").asText().trim();

        } catch (Exception e) {
            log.error("FastAPI 호출 중 오류 발생", e);
            throw new RuntimeException("FastAPI 요청 실패", e);
        }
    }

    private Chat saveUserMessageToRedis(Long carOwnerId, ChatbotRequest request) {
        Chat userMessage = Chat.builder()
                .id(generateUniqueId())
                .carOwnerId(carOwnerId)
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .sender(SenderType.USER)
                .build();

        redisChatService.saveChat(userMessage);
        return userMessage;
    }

    private Chat saveBotMessageToRedis(Long carOwnerId, String content) {
        Chat botMessage = Chat.builder()
                .id(generateUniqueId())
                .carOwnerId(carOwnerId)
                .message(content)
                .createdAt(LocalDateTime.now())
                .sender(SenderType.BOT)
                .build();

        redisChatService.saveChat(botMessage);
        return botMessage;
    }

    private String generateUniqueId() {
        return UUID.randomUUID().toString();
    }

    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public void endChatSession(Long carOwnerId) {
        log.info("Ending chat session for carOwnerId: {}", carOwnerId);

        List<Chat> pendingChats = redisChatService.getPendingChats(carOwnerId);

        if (!pendingChats.isEmpty()) {
            chatRepository.saveAll(pendingChats);
            redisChatService.clearPendingChats(carOwnerId);
            log.info("Saved {} pending chats to DB for carOwnerId: {}", pendingChats.size(), carOwnerId);
        }
    }
}
