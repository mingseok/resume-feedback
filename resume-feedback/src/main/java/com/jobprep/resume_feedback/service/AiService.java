package com.jobprep.resume_feedback.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AiService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    @Value("${spring.ai.openai.model}")
    private String model;
    private final ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build(); // ChatClient 인스턴스 생성
    }

    @Async
    public CompletableFuture<String> getFeedbackForQuestionAsync(String question, String content) {
        long startTime = System.currentTimeMillis();
        System.out.println("비동기 시작: " + question + " at " + startTime + " | Thread: " + Thread.currentThread().getName());

        String response = chatClient.prompt()
                .user(question + "\n\n이력서 내용:\n" + content)
                .call()
                .content();

        long endTime = System.currentTimeMillis();
        System.out.println("비동기 종료: " + question + " at " + endTime + " (처리 시간: " + (endTime - startTime) + "ms) | Thread: " + Thread.currentThread().getName());
        return CompletableFuture.completedFuture(response);
    }

    public CompletableFuture<Map<String, String>> getFeedbackForSectionsAsync(String content) {
        String preprocessedContent = preprocessResumeText(content);

        String[] questions = {
                "이력서의 기본 정보(이름, 연락처, 이메일 등)가 적절하게 포함되고 형식이 올바른지 평가해주세요.",
                "기술 스택이 직무에 적합하고 충분히 설명되었는지 평가해주세요.",
                "경력 사항과 포트폴리오가 직무와 연관성이 높고 주요 성과가 잘 드러나 있는지 평가해주세요.",
                "대외활동과 자격증이 직무와 관련성이 있으며, 지원자의 역량을 보완하는지 평가해주세요.",
                "자기소개서가 직무와 적합하고, 지원자의 강점과 가치관을 잘 전달하고 있는지 평가해주세요."
        };

        List<CompletableFuture<Map.Entry<String, String>>> futures =
                List.of(questions).stream()
                        .map(question -> getFeedbackForQuestionAsync(question, preprocessedContent)
                                .thenApply(response -> Map.entry(question, response)))
                        .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private String preprocessResumeText(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "입력된 텍스트가 없습니다. 유효한 텍스트를 입력해주세요.";
        }
        String cleanedText = rawText.replaceAll("[^가-힣a-zA-Z0-9.,!?\\s]", " ");
        return cleanedText.replaceAll("\\s+", " ").trim();
    }
}
