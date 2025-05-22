package com.ll.carjini.domain.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.carjini.domain.chatbot.dto.ChatbotRequest;
import com.ll.carjini.domain.chatbot.dto.ChatbotResponse;
import com.ll.carjini.domain.chatbot.entity.Chat;
import com.ll.carjini.domain.chatbot.entity.SenderType;
import com.ll.carjini.domain.chatbot.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
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
@Primary
public class ChatbotServiceImpl implements ChatbotService {

    private final RestTemplate restTemplate;
    private final ChatRepository chatRepository;
    private final RedisChatService redisChatService;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String openAiModel;

    @Override
    public ChatbotResponse processMessage(Long carOwnerId, ChatbotRequest request) {
        saveUserMessageToRedis(carOwnerId, request);

        String botResponseText = callChatGptApi( carOwnerId, request.getMessage());

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

    private String callChatGptApi(Long carOwnerId, String userMessage) {
        log.info("Calling ChatGPT API with message: {}", userMessage);

        List<Chat> recentChats = redisChatService.getRecentChats(carOwnerId, 3);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        String systemPrompt = "You are an KOREAN automotive expert chatbot designed to help customers with a wide range of car-related questions. Answer in Korean. \n" +
                "Your role is to provide friendly, accurate, and professional advice on topics such as:\n" +
                "\n" +
                "- Car maintenance and repair\n" +
                "- Vehicle recommendations based on user needs\n" +
                "- Engine performance and fuel efficiency\n" +
                "- Troubleshooting mechanical or electronic issues\n" +
                "- Differences between car models, trims, and years\n" +
                "- Car buying advice (new vs. used, dealership vs. private)\n" +
                "- Safe driving practices and seasonal vehicle care\n" +
                "\n" +
                "Speak in a clear and helpful tone, like a knowledgeable mechanic or car advisor.\n" +
                "Ask follow-up questions if needed to better understand the customer's issue.\n" +
                "Avoid technical jargon unless the user seems to understand it.\n" +
                "If a question is outside your scope (e.g., legal or financial advice), politely suggest consulting a specialist.\n" +
                "\n" +
                "Always prioritize safety, accuracy, and customer satisfaction.";

        StringBuilder messagesJson = new StringBuilder();
        messagesJson.append("[\n");
        messagesJson.append(" {\"role\": \"system\", \"content\": \"").append(escapeJsonString(systemPrompt)).append("\"},\n");

        for (int i = recentChats.size() - 1; i >= 0; i--) {
            Chat chat = recentChats.get(i);
            String role = chat.getSender() == SenderType.USER ? "user" : "assistant";
            messagesJson.append(" {\"role\": \"").append(role).append("\", \"content\": \"")
                    .append(escapeJsonString(chat.getMessage())).append("\"},\n");
        }

        messagesJson.append(" {\"role\": \"user\", \"content\": \"").append(escapeJsonString(userMessage)).append("\"}\n");
        messagesJson.append(" ]");

        String requestBody = String.format(
                "{\n" +
                        " \"model\": \"%s\",\n" +
                        " \"messages\": %s,\n" +
                        " \"temperature\": 0\n" +
                        "}",
                openAiModel,
                messagesJson);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    openAiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            String responseBody = responseEntity.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            return rootNode.path("choices").get(0).path("message").path("content").asText().trim();
        } catch (Exception e) {
            log.error("Error calling ChatGPT API", e);
            throw new RuntimeException("ChatGPT API 요청 실패", e);
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