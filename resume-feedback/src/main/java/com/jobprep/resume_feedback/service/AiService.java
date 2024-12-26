package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AiService {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().executor(executorService).build();

    private final AtomicLong totalResponseTime = new AtomicLong(0); // 총 응답 시간
    private final AtomicLong requestCount = new AtomicLong(0);      // 요청 횟수

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    @Value("${spring.ai.openai.model}")
    private String model;

    public CompletableFuture<String> getFeedbackForQuestionAsync(String question, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 요청 본문 생성
                Map<String, Object> requestPayload = new HashMap<>();
                requestPayload.put("model", model);

                Map<String, String> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", question + "\n\n" + content);
                requestPayload.put("messages", new Map[]{message});

                return objectMapper.writeValueAsString(requestPayload);
            } catch (Exception e) {
                throw new RuntimeException("JSON 직렬화 중 오류 발생: " + e.getMessage(), e);
            }
        }).thenCompose(requestBody -> {
            // HTTP 요청 생성
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            long startTime = System.currentTimeMillis(); // **요청 시작 시간 측정**

            // 논블로킹 HTTP 요청 전송
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        long endTime = System.currentTimeMillis(); // **요청 종료 시간 측정**
                        long responseTime = endTime - startTime;    // **응답 시간 계산**

                        // **응답 시간 누적 및 평균 계산**
                        totalResponseTime.addAndGet(responseTime);
                        long currentCount = requestCount.incrementAndGet();
                        long averageResponseTime = totalResponseTime.get() / currentCount;

                        // **콘솔에 출력**
                        System.out.println("요청 응답 시간: " + responseTime + "ms");
                        System.out.println("현재까지의 평균 응답 시간: " + averageResponseTime + "ms");


                        // 응답 데이터 추출
                        try {
                            JsonNode root = objectMapper.readTree(response.body());
                            return root.path("choices").get(0).path("message").path("content").asText();
                        } catch (Exception e) {
                            throw new RuntimeException("응답 데이터 파싱 중 오류 발생: " + e.getMessage(), e);
                        }
                    });
        });
    }

    public CompletableFuture<Map<String, String>> getFeedbackForSectionsAsync(String content) {
        String[] questions = {
                "이력서의 기본 정보(이름, 연락처, 이메일 등)가 적절하게 포함되고 형식이 올바른지 평가해주세요.",
                "기술 스택이 직무에 적합하고 충분히 설명되었는지 평가해주세요.",
                "경력 사항과 포트폴리오가 직무와 연관성이 높고 주요 성과가 잘 드러나 있는지 평가해주세요.",
                "대외활동과 자격증이 직무와 관련성이 있으며, 지원자의 역량을 보완하는지 평가해주세요.",
                "자기소개서가 직무와 적합하고, 지원자의 강점과 가치관을 잘 전달하고 있는지 평가해주세요."
        };

        CompletableFuture<Map<String, String>> resultFuture = CompletableFuture.supplyAsync(HashMap::new);

        for (String question : questions) {
            resultFuture = resultFuture.thenCombine(
                    getFeedbackForQuestionAsync(question, content),
                    (results, feedback) -> {
                        results.put(question, feedback);
                        return results;
                    }
            );
        }

        return resultFuture;
    }
}