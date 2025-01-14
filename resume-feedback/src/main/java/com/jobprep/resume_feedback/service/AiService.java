package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobprep.resume_feedback.domain.Resume;
import com.jobprep.resume_feedback.dto.FeedbackResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AiService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    public CompletableFuture<FeedbackResponseDto> generateFeedback(Resume resume) {
        String prompt = createPrompt(resume);

        return sendOpenAiRequest(prompt)
                .thenApply(this::parseResponse)
                .exceptionally(ex -> {
                    System.err.println("오류 발생: " + ex.getMessage());
                    ex.printStackTrace();
                    return new FeedbackResponseDto("자기소개 부족", "기술 스택 부족", "경력 부족", "프로젝트 부족", "대외활동 부족");
                });
    }

//    private String createPrompt(Resume resume) {
//        return """
//            이력서를 검토하고 다음 항목별로 피드백을 주세요.
//            각 항목에 대해 글자 수 130자 정도로 피드백을 작성해줘:
//            1. 기본 정보
//            2. 자기소개
//            3. 기술 스택
//            4. 경력
//            5. 프로젝트
//            6. 포트폴리오
//            7. 대외활동
//            """ + resume.toString();
//    }

    private String createPrompt(Resume resume) {
        return """
        이력서를 검토하고 항목별로 구체적이고 간결한 피드백을 주세요.
        각 항목에 대해 최대 5줄로 작성하고, 개선점과 강점을 함께 제시해 주세요.
        
        [이력서 항목]
        1. 자기소개
        2. 기술 스택
        3. 경력
        4. 프로젝트
        5. 대외활동

        아래는 검토할 이력서 내용입니다:
        """ + resume.toString();
    }

    private CompletableFuture<String> sendOpenAiRequest(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", Map.of("role", "user", "content", prompt));
            requestBody.put("max_tokens", 500);

            String requestBodyString = new ObjectMapper().writeValueAsString(requestBody);

            // 디버깅용 출력
            System.out.println("Request Body: " + requestBodyString);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        System.out.println("Response Body: " + response.body());
                        return response.body();
                    });
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API 요청 실패", e);
        }
    }

    private FeedbackResponseDto parseResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            String content = extractContentFromResponse(responseBody);
            Map<String, String> feedbackMap = extractFeedbackByCategory(content);

            // 디버깅용 출력
            System.out.println("파싱된 피드백 내용: " + feedbackMap);

            // FeedbackResponseDto 생성 시 파싱된 피드백 적용
            return new FeedbackResponseDto(
                    feedbackMap.getOrDefault("자기소개", "자기소개 없음"),
                    feedbackMap.getOrDefault("기술 스택", "기술 스택 없음"),
                    feedbackMap.getOrDefault("경력", "경력 없음"),
                    feedbackMap.getOrDefault("프로젝트", "프로젝트 없음"),
                    feedbackMap.getOrDefault("대외활동", "대외활동 없음")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new FeedbackResponseDto("자기소개 없음", "기술 스택 없음", "경력 없음", "프로젝트 없음", "대외활동 없음");
        }
    }

    private String extractContentFromResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // 응답 구조가 올바른지 확인 후 값 추출
            if (rootNode.has("choices") && rootNode.get("choices").isArray()) {
                return rootNode.get("choices").get(0).get("message").get("content").asText();
            } else {
                throw new RuntimeException("OpenAI 응답 구조가 예상과 다릅니다.");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("OpenAI 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private Map<String, String> extractFeedbackByCategory(String content) {
        Map<String, String> feedbackMap = new HashMap<>();
        String[] lines = content.split("\n");

        String currentCategory = null;
        StringBuilder feedbackBuilder = new StringBuilder();

        for (String line : lines) {
            // 카테고리 번호로 시작하는 라인을 구분하여 카테고리 설정
            if (line.matches("^\\d+\\..*")) {
                // 기존 카테고리의 피드백을 저장
                if (currentCategory != null) {
                    feedbackMap.put(currentCategory.trim(), feedbackBuilder.toString().trim());
                }
                // 새로운 카테고리 시작
                currentCategory = line.split("\\.", 2)[1].trim();
                feedbackBuilder.setLength(0);  // StringBuilder 초기화
            } else if (currentCategory != null) {
                feedbackBuilder.append(line).append(" ");
            }
        }

        // 마지막 카테고리 저장
        if (currentCategory != null) {
            feedbackMap.put(currentCategory.trim(), feedbackBuilder.toString().trim());
        }

        // 🔧 디버깅 코드
        System.out.println("🔧 파싱된 피드백 키 목록: " + feedbackMap.keySet());
        System.out.println("🔧 파싱된 피드백 내용: " + feedbackMap);

        return feedbackMap;
    }
}
