package com.ll.carjini.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateRequest {
    private String inputs;
    private Parameters parameters;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Parameters {
        @JsonProperty("max_new_tokens")
        private int maxNewTokens = 512;
        private double temperature = 0.7;
        @JsonProperty("top_p")
        private double topP = 0.8;
        @JsonProperty("do_sample")
        private boolean doSample = true;
    }
}