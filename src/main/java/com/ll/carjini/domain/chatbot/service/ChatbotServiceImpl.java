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

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private final RestTemplate restTemplate;
    private final ChatRepository chatRepository;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiUrl;

    @Value("${openai.api.model:gpt-4o-mini}")
    private String openAiModel;

    @Override
    public ChatbotResponse processMessage(ChatbotRequest request) {
        // 사용자 메시지 저장
        saveUserMessage(request);

        // ChatGPT API를 통해 응답 생성 (이전 대화 내역 포함)
        String botResponseText = callChatGptApi(request.getMessage(), request.getCarOwnerId());

        // 챗봇 응답 저장
        Chat botMessage = saveBotMessage(request.getCarOwnerId(), botResponseText);

        // 응답 생성 및 반환
        return ChatbotResponse.builder()
                .carOwnerId(request.getCarOwnerId())
                .message(botResponseText)
                .createdAt(botMessage.getCreatedAt())
                .build();
    }

    @Override
    public Page<Chat> getChatHistoryPaginated(Long carOwnerId, Pageable pageable) {
        log.info("Fetching paginated chatbot history for user: {}, pageable: {}", carOwnerId, pageable);
        return chatRepository.findByCarOwnerId(carOwnerId, pageable);
    }

    // ChatGPT API 호출을 별도 메서드로 분리
    private String callChatGptApi(String userMessage, Long carOwnerId) {
        log.info("Calling ChatGPT API with message: {}", userMessage);

        // 이전 대화 내역 3개 조회
        List<Chat> recentChats = chatRepository.findTop3ByCarOwnerIdOrderByCreatedAtDesc(carOwnerId);

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

        // JSON 요청 생성을 위한 StringBuilder
        StringBuilder messagesJson = new StringBuilder();
        messagesJson.append("[\n");
        messagesJson.append(" {\"role\": \"system\", \"content\": \"").append(escapeJsonString(systemPrompt)).append("\"},\n");

        // 이전 대화 내역 추가 (시간순으로 정렬하기 위해 역순으로)
        for (int i = recentChats.size() - 1; i >= 0; i--) {
            Chat chat = recentChats.get(i);
            String role = chat.getSender() == SenderType.USER ? "user" : "assistant";
            messagesJson.append(" {\"role\": \"").append(role).append("\", \"content\": \"")
                    .append(escapeJsonString(chat.getMessage())).append("\"},\n");
        }

        // 현재 사용자 메시지 추가
        messagesJson.append(" {\"role\": \"user\", \"content\": \"").append(escapeJsonString(userMessage)).append("\"}\n");
        messagesJson.append(" ]");

        String requestBody = String.format(
                "{\n" +
                        " \"model\": \"%s\",\n" +
                        " \"messages\": %s,\n" +
                        " \"temperature\": 0\n" +
                        "}",
                openAiModel,
                messagesJson.toString());

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

    // 사용자 메시지 저장 메서드
    private Chat saveUserMessage(ChatbotRequest request) {
        Chat userMessage = Chat.builder()
                .carOwnerId(request.getCarOwnerId())
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())
                .sender(SenderType.USER)
                .build();

        return chatRepository.save(userMessage);
    }

    // 챗봇 응답 저장 메서드
    private Chat saveBotMessage(Long carOwnerId, String content) {
        Chat botMessage = Chat.builder()
                .carOwnerId(carOwnerId)
                .message(content)
                .createdAt(LocalDateTime.now())
                .sender(SenderType.BOT)
                .build();

        return chatRepository.save(botMessage);
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

}