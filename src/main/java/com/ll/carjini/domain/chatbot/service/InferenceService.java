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

    @Value("${app.config.hf-hub-token}")
    private String hfHubToken;

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


    private final String STARIA_SYSTEM_PROMPT = "당신은 현대자동차 스타리아 전문가입니다. 현대 그랜저와 관련된 모든 질문에 대해, 정확하고 친절하게 답변해야 합니다. 외부 지식이 있을 시, 해당 외부 지식을 참고하여 답변해 주세요.만약 외부 지식에 해당 질문에 대한 정보가 없더라도, 당신이 보유한 일반 지식을 바탕으로 유용한 답변을 제공하십시오. 다만, 만약 일반 지식에도 명확한 답변이 없다면, '제공된 내용에 없습니다.'라고 답변해 주세요.";
    private final String GRANDEUR_SYSTEM_PROMPT = "당신은 현대자동차 그랜저 전문가입니다. 현대 그랜저와 관련된 모든 질문에 대해, 정확하고 친절하게 답변해야 합니다. 외부 지식이 있을 시, 해당 외부 지식을 참고하여 답변해 주세요.만약 외부 지식에 해당 질문에 대한 정보가 없더라도, 당신이 보유한 일반 지식을 바탕으로 유용한 답변을 제공하십시오. 다만, 만약 일반 지식에도 명확한 답변이 없다면, '제공된 내용에 없습니다.'라고 답변해 주세요.";

    public InferenceService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.findSimilarSemaphore = new Semaphore(1); // GPU A용
        this.generateAnswerSemaphore = new Semaphore(1); // GPU B용
    }

    @PostConstruct
    public void initialize() {
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
        log.info("Processing Staria query: {}", query);

        try {
            String context = findStariaSimilarContext(query);
            log.info("Found context for query '{}': {}", query, context);
            String rawAnswer = generateStariaAnswer(STARIA_SYSTEM_PROMPT, query, context, history);
            log.info("Generated raw answer for query '{}': {}", query, rawAnswer);
            return postProcessAnswer(rawAnswer, "");
        } catch (Exception e) {
            log.error("Error occurred while processing query", e);
            throw e;
        }
    }


    public String processGrandeurQuery(String query, List<Chat> history) throws Exception {
        String context = findGrandeurSimilarContext(query);
        String rawAnswer = generateGrandeurAnswer(GRANDEUR_SYSTEM_PROMPT, query, context, history);
        return postProcessAnswer(rawAnswer, "");
    }

    private String findStariaSimilarContext(String query) throws Exception {
        if (stariaEmbeddingMap == null || stariaEmbeddingMap.isEmpty()) {
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

            if (bestScore >= 0.75 && bestKey != null) {
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

            String bestKey = null;
            double bestScore = 0.0;

            for (Map.Entry<String, double[]> entry : grandeurEmbeddingMap.entrySet()) {
                double score = cosineSimilarity(queryEmbedding, entry.getValue());
                if (score > bestScore) {
                    bestScore = score;
                    bestKey = entry.getKey();
                }
            }

            if (bestScore >= 0.75 && bestKey != null) {
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
        HttpHeaders headers = createHeaders();
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
        HttpHeaders headers = createHeaders();
        EmbedRequest request = new EmbedRequest(text);
        HttpEntity<EmbedRequest> entity = new HttpEntity<>(request, headers);

        Map<String, double[]> response = restTemplate.exchange(
                grandeurEmbedApiUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, double[]>>() {}
        ).getBody();

        double[] embedding = response.values().iterator().next(); // 첫 번째 값 꺼내기
        return normalizeEmbedding(embedding);
    }

    private String generateStariaAnswer(String systemPrompt, String query, String context, List<Chat> conversationHistory) throws InterruptedException {
        generateAnswerSemaphore.acquire();
        try {
            String fullPrompt = buildPrompt(systemPrompt, query, context, conversationHistory);

            HttpHeaders headers = createHeaders();
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

            HttpHeaders headers = createHeaders();
            GenerateRequest.Parameters parameters = new GenerateRequest.Parameters();
            GenerateRequest request = new GenerateRequest(fullPrompt, parameters);
            HttpEntity<GenerateRequest> entity = new HttpEntity<>(request, headers);

            JsonNode response = restTemplate.postForObject(grandeurApiUrl, entity, JsonNode.class);
            return extractGeneratedText(response);
        } finally {
            generateAnswerSemaphore.release();
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(hfHubToken);
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

    public String postProcessAnswer(String raw, String prevAnswer) {
        if (raw == null || raw.isEmpty()) {
            return "제공된 답변이 없습니다.";
        }

        if (prevAnswer == null) {
            prevAnswer = "";
        }

        // Python: m = re.search(r"\<\|start_header_id\>assistant\<\|end_header_id\>(.*?)\<\|eot_id\>", raw, re.DOTALL)
        Pattern pattern = Pattern.compile(
                "<\\|start_header_id\\>assistant<\\|end_header_id\\>(.*?)<\\|eot_id\\>",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(raw);

        String ans;
        // Python: if m: ans = m.group(1).strip() else: ans = raw.strip()
        if (matcher.find()) {
            ans = matcher.group(1).trim();
        } else {
            ans = raw.trim();
        }

        // Python: ans = re.sub(r"\<\|.*?\|\>", "", ans).strip()
        ans = ans.replaceAll("<\\|.*?\\|>", "").trim();

        // Python: if ans.lower().count("assistant") >= 4: return "제공된 답변이 없습니다."
        String lowerAns = ans.toLowerCase();
        int assistantCount = 0;
        int index = 0;
        while ((index = lowerAns.indexOf("assistant", index)) != -1) {
            assistantCount++;
            index += "assistant".length();
        }

        if (assistantCount >= 4) {
            return "제공된 답변이 없습니다.";
        }

        // Python: if not ans or ans == prev_answer.strip(): return "제공된 답변이 없습니다."
        if (ans.isEmpty() || ans.equals(prevAnswer.trim())) {
            return "제공된 답변이 없습니다.";
        }

        // Python: if len(ans) >= 500: ...
        if (ans.length() >= 500) {
            // Python: sentences = re.split(r'(?<=\.)', ans)
            String[] sentences = ans.split("(?<=\\.)");

            // Python: if sentences and not sentences[-1].strip().endswith('.'):
            if (sentences.length > 0 && !sentences[sentences.length - 1].trim().endsWith(".")) {
                // Python: sentences = sentences[:-1]
                sentences = Arrays.copyOf(sentences, sentences.length - 1);
            }

            // Python: ans = ''.join(sentences).strip()
            ans = String.join("", sentences).trim();
        }

        // Python: return ans
        return ans;
    }

    private void loadStariaEmbeddings() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("staria_qa_and_rag_keys_embed.json");
            stariaEmbeddingMap = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, double[]>>() {});
        } catch (Exception e) {
            System.err.println("Could not load embeddings file: " + e.getMessage());
            stariaEmbeddingMap = Map.of();
        }
    }

    private void loadStariaValueSegments() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("staria_qa_and_rag_values.json");
            stariaValueMap = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, ValueSegment>>() {});
        } catch (Exception e) {
            System.err.println("Could not load value segments file: " + e.getMessage());
            stariaValueMap = Map.of();
        }
    }

    private void loadGrandeurEmbeddings() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("staria_qa_and_rag_keys_embed.json");
            grandeurEmbeddingMap = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, double[]>>() {});
        } catch (Exception e) {
            System.err.println("Could not load embeddings file: " + e.getMessage());
            grandeurEmbeddingMap = Map.of();
        }
    }

    private void loadGrandeurValueSegments() throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("staria_qa_and_rag_values.json");
            grandeurValueMap = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<Map<String, ValueSegment>>() {});
        } catch (Exception e) {
            System.err.println("Could not load value segments file: " + e.getMessage());
            grandeurValueMap = Map.of();
        }
    }
}
