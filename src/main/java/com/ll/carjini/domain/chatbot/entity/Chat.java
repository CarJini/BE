package com.ll.carjini.domain.chatbot.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "chat_history")
public class Chat {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private SenderType sender;  // USER 또는 BOT

    private String message;

    private Long carOwnerId;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    public Chat(SenderType sender, String message, Long carOwnerId) {
        this.sender = sender;
        this.message = message;
        this.carOwnerId = carOwnerId;
    }

}
