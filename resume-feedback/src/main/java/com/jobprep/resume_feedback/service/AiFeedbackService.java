package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobprep.resume_feedback.domain.Resume;
import com.jobprep.resume_feedback.dto.FeedbackResponseDto;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    @Value("${spring.ai.openai.model}")
    private String model;

    /**
     * OpenAI를 호출하여 비동기로 피드백 요청
     *
     * @param resume 파싱된 Resume 객체
     * @return CompletableFuture<FeedbackResponseDto> 피드백 응답
     */
    public CompletableFuture<FeedbackResponseDto> requestFeedback(Resume resume) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> requestBody = buildRequestBody(resume);
            return executeHttpRequest(requestBody);
        });
    }

    /**
     * OpenAI 요청 바디 생성
     *
     * @param resume 이력서 객체
     * @return Map<String, Object> 요청 바디
     */
    private Map<String, Object> buildRequestBody(Resume resume) {
        String prompt = createPrompt(resume);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a professional resume reviewer."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 1000);
        return requestBody;
    }

    /**
     * OpenAI 요청 프롬프트 생성
     *
     * @param resume 이력서 객체
     * @return String 생성된 프롬프트
     */
    private String createPrompt(Resume resume) {
        return """
                아래는 이력서 검토 요청입니다. 각 항목에 대해 간결하고 구체적인 피드백을 주세요. 각 항목은 16줄 이내로 작성해주세요:

                1. 자기소개: 본인의 강점과 역할을 중심으로 소개 내용을 작성해주세요.
                2. 기술 스택: 사용한 기술이 어떤지 설명해주세요.
                3. 경력: 과거 직무에서 수행한 역할과 성과를 중심으로 작성해주세요.
                4. 프로젝트: 수행한 프로젝트의 주요 내용, 본인의 기여도 및 성과를 설명해주세요.
                5. 대외활동: 참여한 활동과 이를 통해 얻은 경험 및 성장 내용을 작성해주세요.

                이력서 내용:
                """ + resume.toString();
    }

    /**
     * HTTP 요청 실행
     *
     * @param requestBody 요청 바디
     * @return FeedbackResponseDto 피드백 응답 객체
     */
    private FeedbackResponseDto executeHttpRequest(Map<String, Object> requestBody) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = createHttpPost(requestBody);
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes());
                return parseOpenAiResponse(responseBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("HTTP 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * HTTP POST 객체 생성
     *
     * @param requestBody 요청 바디
     * @return HttpPost 생성된 HTTP POST 객체
     */
    private HttpPost createHttpPost(Map<String, Object> requestBody) throws IOException {
        HttpPost post = new HttpPost(apiUrl);
        post.setHeader("Authorization", "Bearer " + apiKey);
        post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(requestBody), ContentType.APPLICATION_JSON));
        return post;
    }

    /**
     * OpenAI 응답 파싱
     *
     * @param responseBody 응답 본문
     * @return FeedbackResponseDto 파싱된 응답 객체
     */
    private FeedbackResponseDto parseOpenAiResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            String content = rootNode
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            Map<String, String> feedbackMap = extractFeedback(content);
            return buildFeedbackResponse(feedbackMap);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI 응답 파싱 오류: " + e.getMessage(), e);
        }
    }

    /**
     * OpenAI 응답에서 항목 추출
     *
     * @param content 응답 내용
     * @return Map<String, String> 추출된 항목 맵
     */
    private Map<String, String> extractFeedback(String content) {
        Map<String, String> feedbackMap = new HashMap<>();
        Pattern pattern = Pattern.compile("(?m)^([가-힣a-zA-Z ]+):\\s*(.*?)(?=^[가-힣a-zA-Z ]+:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            feedbackMap.put(matcher.group(1).trim(), matcher.group(2).trim());
        }

        return feedbackMap;
    }

    /**
     * 피드백 DTO 생성
     *
     * @param feedbackMap 추출된 피드백 맵
     * @return FeedbackResponseDto 생성된 피드백 DTO
     */
    private FeedbackResponseDto buildFeedbackResponse(Map<String, String> feedbackMap) {
        return new FeedbackResponseDto(
                feedbackMap.getOrDefault("자기소개", "자기소개 없음"),
                feedbackMap.getOrDefault("기술 스택", "기술 스택 없음"),
                feedbackMap.getOrDefault("경력", "경력 없음"),
                feedbackMap.getOrDefault("프로젝트", "프로젝트 없음"),
                feedbackMap.getOrDefault("대외활동", "대외활동 없음")
        );
    }
}
