package com.ll.carjini.domain.chatbot.service;

import com.ll.carjini.domain.chatbot.entity.Chat;
import com.ll.carjini.domain.chatbot.dto.EmbedRequest;
import com.ll.carjini.domain.chatbot.dto.GenerateRequest;
import com.ll.carjini.domain.chatbot.dto.ValueSegment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class InferenceService {

    @Value("${app.config.staria-hf-hub-token}")
    private String stariaHfHubToken;

    @Value("${app.config.grandeur-hf-hub-token}")
    private String grandeurHfHubToken;

    @Value("${app.config.staria-api-url}")
    private String stariaApiUrl;

    @Value("${app.config.staria-embed-api-url}")
    private String stariaEmbedApiUrl;

    @Value("${app.config.grandeur-api-url}")
    private String grandeurApiUrl;

    @Value("${app.config.grandeur-embed-api-url}")
    private String grandeurEmbedApiUrl;


    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Semaphore findSimilarSemaphore;
    private final Semaphore generateAnswerSemaphore;

    private Map<String, double[]> stariaEmbeddingMap;
    private Map<String, ValueSegment> stariaValueMap;
    private Map<String, double[]> grandeurEmbeddingMap;
    private Map<String, ValueSegment> grandeurValueMap;


    private final String STARIA_SYSTEM_PROMPT = "당신은 현대자동차 스타리아 전문가입니다. 현대 스타리아와 관련된 모든 질문에 대해, 정확하고 친절하게 답변해야 합니다. 외부 지식이 있을 시, 해당 외부 지식을 참고하여 답변해 주세요. 만약 외부 지식에 해당 질문에 대한 정보가 없더라도, 당신이 보유한 일반 지식을 바탕으로 유용한 답변을 제공하십시오. 다만, 만약 일반 지식에도 명확한 답변이 없다면, \"제공된 내용에 없습니다.\"라고 답변해 주세요.";
    private final String GRANDEUR_SYSTEM_PROMPT = "당신은 현대자동차 그랜저 전문가입니다. 현대 그랜저와 관련된 모든 질문에 대해, 정확하고 친절하게 답변해야 합니다. 외부 지식이 있을 시, 해당 외부 지식을 참고하여 답변해 주세요.만약 외부 지식에 해당 질문에 대한 정보가 없더라도, 당신이 보유한 일반 지식을 바탕으로 유용한 답변을 제공하십시오. 다만, 만약 일반 지식에도 명확한 답변이 없다면, \"제공된 내용에 없습니다.\"라고 답변해 주세요.";

    public InferenceService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.findSimilarSemaphore = new Semaphore(1); // GPU A용
        this.generateAnswerSemaphore = new Semaphore(1); // GPU B용
    }

    @PostConstruct
    public void initialize() {
        log.info("initialize() called");
        try {
            loadStariaEmbeddings();
            loadStariaValueSegments();
            loadGrandeurEmbeddings();
            loadGrandeurValueSegments();
        } catch (Exception e) {
            System.err.println("Failed to load data files: " + e.getMessage());
        }
    }

    public String processStariaQuery(String query, List<Chat> history) throws Exception {
        try {
            String context = findStariaSimilarContext(query);
            log.info("context: {}", context);

            String rawAnswer = generateStariaAnswer(STARIA_SYSTEM_PROMPT, query, context, history);
            return postProcessAnswer(rawAnswer, "");
        } catch (Exception e) {
            throw e;
        }
    }


    public String processGrandeurQuery(String query, List<Chat> history) throws Exception {
        String context = findGrandeurSimilarContext(query);
        log.info("context: {}", context);
        String rawAnswer = generateGrandeurAnswer(GRANDEUR_SYSTEM_PROMPT, query, context, history);
        return postProcessAnswer(rawAnswer, "");
    }

    private String findStariaSimilarContext(String query) throws Exception {
        if (stariaEmbeddingMap == null || stariaEmbeddingMap.isEmpty()) {
            log.info("Staria embedding map is empty or not loaded.");
            return "";
        }

        try {
            double[] queryEmbedding = getStariaQueryEmbedding(query);

            String bestKey = null;
            double bestScore = 0.0;

            for (Map.Entry<String, double[]> entry : stariaEmbeddingMap.entrySet()) {
                double score = cosineSimilarity(queryEmbedding, entry.getValue());
                if (score > bestScore) {
                    bestScore = score;
                    bestKey = entry.getKey();
                }
            }

            if (bestScore >= 0.65 && bestKey != null) {
                return extractStariaContextFromKey(bestKey);
            }

            return "";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String findGrandeurSimilarContext(String query) throws Exception {
        if (grandeurEmbeddingMap == null || grandeurEmbeddingMap.isEmpty()) {
            return "";
        }

        try {
            double[] queryEmbedding = getGrandeurQueryEmbedding(query);
            log.info("queryEmbedding: {}", Arrays.toString(queryEmbedding));

            String bestKey = null;
            double bestScore = 0.0;

            for (Map.Entry<String, double[]> entry : grandeurEmbeddingMap.entrySet()) {
                double score = cosineSimilarity(queryEmbedding, entry.getValue());
                if (score > bestScore) {
                    bestScore = score;
                    bestKey = entry.getKey();
                }
            }

            if (bestScore >= 0.65 && bestKey != null) {
                return extractGrandeurContextFromKey(bestKey);
            }

            return "";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extractStariaContextFromKey(String fullKey) throws InterruptedException {
        findSimilarSemaphore.acquire();
        try {
            String[] parts = fullKey.split("_");
            if (parts.length >= 2) {
                String fileKey = String.join("_", java.util.Arrays.copyOf(parts, parts.length - 1));
                String segId = parts[parts.length - 1];

                ValueSegment segment = stariaValueMap.get(fileKey);
                if (segment != null && segment.getSegments() != null) {
                    return segment.getSegments().getOrDefault(segId, "");
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting context: " + e.getMessage());
        } finally {
            findSimilarSemaphore.release();
        }
        return "";
    }

    private String extractGrandeurContextFromKey(String fullKey) throws InterruptedException {
        findSimilarSemaphore.acquire();
        try {
            String[] parts = fullKey.split("_");
            if (parts.length >= 2) {
                String fileKey = String.join("_", java.util.Arrays.copyOf(parts, parts.length - 1));
                String segId = parts[parts.length - 1];

                ValueSegment segment = grandeurValueMap.get(fileKey);
                if (segment != null && segment.getSegments() != null) {
                    return segment.getSegments().getOrDefault(segId, "");
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting context: " + e.getMessage());
        } finally {
            findSimilarSemaphore.release();
        }
        return "";
    }

    private double[] getStariaQueryEmbedding(String text) {
        HttpHeaders headers = createStariaHeaders();
        EmbedRequest request = new EmbedRequest(text);
        HttpEntity<EmbedRequest> entity = new HttpEntity<>(request, headers);

        ParameterizedTypeReference<List<List<Double>>> typeRef =
                new ParameterizedTypeReference<>() {};

        List<List<Double>> response = restTemplate.exchange(
                stariaEmbedApiUrl,
                HttpMethod.POST,
                entity,
                typeRef
        ).getBody();

        if (response == null || response.isEmpty()) {
            throw new RuntimeException("Empty embedding response from API");
        }

        // Python의 .squeeze()와 동일 - 첫 번째 임베딩 벡터 추출
        List<Double> embeddingList = response.get(0);

        // List<Double> → double[] 변환 (Python의 .cpu().numpy()와 동일)
        double[] q_emb = embeddingList.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        // Python의 normalized_embedding(q_emb).astype("float32")와 동일
        return normalizeEmbedding(q_emb);
    }


    private double[] getGrandeurQueryEmbedding(String text) {
        HttpHeaders headers = createGrandeurHeaders();
        EmbedRequest request = new EmbedRequest(text);
        HttpEntity<EmbedRequest> entity = new HttpEntity<>(request, headers);

        ParameterizedTypeReference<List<List<Double>>> typeRef =
                new ParameterizedTypeReference<>() {};

        List<List<Double>> response = restTemplate.exchange(
                grandeurEmbedApiUrl,
                HttpMethod.POST,
                entity,
                typeRef
        ).getBody();

        if (response == null || response.isEmpty()) {
            throw new RuntimeException("Empty embedding response from API");
        }

        // Python의 .squeeze()와 동일 - 첫 번째 임베딩 벡터 추출
        List<Double> embeddingList = response.get(0);

        // List<Double> → double[] 변환 (Python의 .cpu().numpy()와 동일)
        double[] q_emb = embeddingList.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        // Python의 normalized_embedding(q_emb).astype("float32")와 동일
        return normalizeEmbedding(q_emb);
    }

    private String generateStariaAnswer(String systemPrompt, String query, String context, List<Chat> conversationHistory) throws InterruptedException {
        generateAnswerSemaphore.acquire();
        try {
            String fullPrompt = buildPrompt(systemPrompt, query, context, conversationHistory);

            HttpHeaders headers = createStariaHeaders();
            GenerateRequest.Parameters parameters = new GenerateRequest.Parameters();
            GenerateRequest request = new GenerateRequest(fullPrompt, parameters);
            HttpEntity<GenerateRequest> entity = new HttpEntity<>(request, headers);

            JsonNode response = restTemplate.postForObject(stariaApiUrl, entity, JsonNode.class);
            return extractGeneratedText(response);
        } finally {
            generateAnswerSemaphore.release();
        }
    }


    private String generateGrandeurAnswer(String systemPrompt, String query, String context, List<Chat> conversationHistory) throws InterruptedException {
        generateAnswerSemaphore.acquire();
        try {
            String fullPrompt = buildPrompt(systemPrompt, query, context, conversationHistory);

            HttpHeaders headers = createGrandeurHeaders();
            GenerateRequest.Parameters parameters = new GenerateRequest.Parameters();
            GenerateRequest request = new GenerateRequest(fullPrompt, parameters);
            HttpEntity<GenerateRequest> entity = new HttpEntity<>(request, headers);

            JsonNode response = restTemplate.postForObject(grandeurApiUrl, entity, JsonNode.class);
            return extractGeneratedText(response);
        } finally {
            generateAnswerSemaphore.release();
        }
    }

    private HttpHeaders createStariaHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(stariaHfHubToken);
        return headers;
    }

    private HttpHeaders createGrandeurHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(grandeurHfHubToken);
        return headers;
    }

    private double[] normalizeEmbedding(double[] embedding) {
        double norm = 0.0;
        for (double value : embedding) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);

        if (norm == 0.0) {
            return embedding;
        }

        double[] normalized = new double[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = embedding[i] / norm;
        }
        return normalized;
    }


    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private String buildPrompt(String systemPrompt, String query, String context, List<Chat> conversationHistory) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("<|begin_of_text|>\n")
                .append("<|start_header_id|>system<|end_header_id|>\n")
                .append(systemPrompt)
                .append("\n<|eot_id|>\n");

        if (conversationHistory != null) {
            for (Chat chat : conversationHistory) {
                String content = chat.getMessage();
                String[] lines = content.split("\n");
                if (lines.length > 0 && lines[0].trim().equalsIgnoreCase(String.valueOf(chat.getSender()))) {
                    content = String.join("\n", java.util.Arrays.copyOfRange(lines, 1, lines.length)).strip();
                }

                if ("USER".equals(chat.getSender())) {
                    promptBuilder.append("<|start_header_id|>user<|end_header_id|>\n")
                            .append(content)
                            .append("\n<|eot_id|>\n");
                } else if ("BOT".equals(chat.getSender())) {
                    promptBuilder.append("<|start_header_id|>assistant<|end_header_id|>\n")
                            .append(content)
                            .append("\n<|eot_id|>\n");
                }
            }
        }

        String userBlock = !context.isEmpty() ? query + "\n\n### 외부 지식 ###\n" + context : query;
        promptBuilder.append("<|start_header_id|>user<|end_header_id|>\n")
                .append(userBlock)
                .append("\n<|eot_id|>\n")
                .append("<|start_header_id|>assistant<|end_header_id|>\n");

        return promptBuilder.toString();
    }

    private String extractGeneratedText(JsonNode response) {
        String text;
        if (response.has("generated_text")) {
            text = response.get("generated_text").asText();
        } else if (response.isArray() && response.size() > 0 && response.get(0).has("generated_text")) {
            text = response.get(0).get("generated_text").asText();
        } else {
            throw new RuntimeException("Unexpected response format: " + response.toString());
        }

        if (text.contains("[답변]")) {
            return text.split("\\[답변\\]", 2)[1].strip();
        }
        return text.strip();
    }

    public String postProcessAnswer(String rawAnswer, String prevAnswer) {
        if (rawAnswer == null || rawAnswer.isEmpty()) {
            return "제공된 답변이 없습니다.";
        }

        // <|start_header_id|>assistant<|end_header_id|> 다음부터 다음 "assistant" 단어 전까지 추출
        String startTag = "<|start_header_id|>assistant<|end_header_id|>";
        int startIndex = rawAnswer.indexOf(startTag);

        if (startIndex == -1) {
            return rawAnswer.trim(); // 태그가 없으면 전체 텍스트 반환
        }

        // 시작 태그 다음부터 시작
        int contentStart = startIndex + startTag.length();
        String remaining = rawAnswer.substring(contentStart);

        // 다음 "assistant" 단어를 찾기
        int nextAssistantIndex = remaining.indexOf("assistant");

        String answer;
        if (nextAssistantIndex != -1) {
            answer = remaining.substring(0, nextAssistantIndex).trim();
        } else {
            answer = remaining.trim();
        }

        // 기타 태그들 제거
        answer = answer.replaceAll("<\\|.*?\\|>", "").trim();

        // Check if "assistant" appears 4 or more times (case insensitive)
        String lowerAns = answer.toLowerCase();
        int assistantCount = 0;
        int index = 0;
        while ((index = lowerAns.indexOf("assistant", index)) != -1) {
            assistantCount++;
            index += "assistant".length();
        }

        if (assistantCount >= 4) {
            return "제공된 답변이 없습니다.";
        }

        // Check if answer is empty or same as previous answer
        String trimmedPrevAnswer = (prevAnswer != null) ? prevAnswer.trim() : "";
        if (answer.isEmpty() || answer.equals(trimmedPrevAnswer)) {
            return "제공된 답변이 없습니다.";
        }

        // Truncate if length >= 500
        if (answer.length() >= 190) {
            String[] sentences = answer.split("(?<=\\.)");
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < sentences.length; i++) {
                // Skip the last sentence if it doesn't end with a period
                if (i == sentences.length - 1 && !sentences[i].trim().endsWith(".")) {
                    break;
                }
                result.append(sentences[i]);
            }

            answer = result.toString().trim();
        }

        return answer;
    }

    private void loadStariaEmbeddings() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("staria_qa_and_rag_keys_embed.json");
            log.info("Loading staria embeddings from: {}", resource.getPath());
            stariaEmbeddingMap = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, double[]>>() {});
            log.info("Loaded Staria embeddings: {}", stariaEmbeddingMap.size());
        } catch (Exception e) {
            System.err.println("Could not load embeddings file: " + e.getMessage());
            stariaEmbeddingMap = Map.of();
        }
    }

    private void loadStariaValueSegments() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("staria_qa_and_rag_values.json");
            log.info("Loading staria value segments from: {}", resource.getPath());
            stariaValueMap = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, ValueSegment>>() {});
            log.debug("DEBUG: Loaded Staria value segments: {}", stariaValueMap.size());

        } catch (Exception e) {
            System.err.println("Could not load value segments file: " + e.getMessage());
            stariaValueMap = Map.of();
        }
    }

    private void loadGrandeurEmbeddings() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("grandeur_qa_and_rag_keys_embed.json");
            log.info("Loading grandeur embeddings from: {}", resource.getPath());
            grandeurEmbeddingMap = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, double[]>>() {});
        } catch (Exception e) {
            System.err.println("Could not load embeddings file: " + e.getMessage());
            grandeurEmbeddingMap = Map.of();
        }
    }

    private void loadGrandeurValueSegments() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("grandeur_qa_and_rag_values.json");
            grandeurValueMap = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, ValueSegment>>() {});
        } catch (Exception e) {
            System.err.println("Could not load value segments file: " + e.getMessage());
            grandeurValueMap = Map.of();
        }
    }
}
