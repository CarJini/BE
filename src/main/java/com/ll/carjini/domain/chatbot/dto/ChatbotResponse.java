package com.ll.carjini.domain.chatbot.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    private String sender;  //USER 인지 BOT인지 구분
    private Long carOwnerId;
    private String message;
    private LocalDateTime createdAt;
}
