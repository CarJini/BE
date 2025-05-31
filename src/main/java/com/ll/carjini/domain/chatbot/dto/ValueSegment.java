package com.ll.carjini.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValueSegment {
    private java.util.Map<String, String> segments;
}
