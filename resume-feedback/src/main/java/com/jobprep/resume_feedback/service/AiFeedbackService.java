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
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    @Value("${spring.ai.openai.model}")
    private String model;

    private final AtomicInteger requestCount = new AtomicInteger(); // 🔥 요청 수 카운트

//  비동기 로직
//    public CompletableFuture<FeedbackResponseDto> requestFeedback(Resume resume) {
//        return CompletableFuture.supplyAsync(() -> {
//            Map<String, Object> requestBody = buildRequestBody(resume);
//            return executeHttpRequest(requestBody);
//        });
//    }




    public FeedbackResponseDto requestFeedback(Resume resume) {
        long startTime = System.nanoTime(); // ⏳ 시작 시간 측정
        requestCount.incrementAndGet();  // 요청 수 증가

        System.out.println("📌 현재 요청 수: " + requestCount.get());

        Map<String, Object> requestBody = buildRequestBody(resume);
        FeedbackResponseDto response = executeHttpRequest(requestBody);

        long endTime = System.nanoTime(); // ⏳ 종료 시간 측정
        double elapsedTime = (endTime - startTime) / 1_000_000.0; // ms 변환

        System.out.println("📌 AI 응답 속도: " + elapsedTime + "ms");
        return response;
    }

//    public FeedbackResponseDto requestFeedback(Resume resume) {
//        Map<String, Object> requestBody = buildRequestBody(resume);
//        return executeHttpRequest(requestBody);
//    }

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

    private String createPrompt(Resume resume) {
        String prompt = """
            당신은 전문적인 이력서 리뷰어입니다.\s
            아래 이력서를 분석하고, 각 항목별로 구체적인 피드백을 JSON 형식으로 제공합니다.

            ### 요청 형식:
            - 각 항목별로 상세한 피드백을 JSON 객체 형태로 작성합니다.
            - 반드시 아래 JSON 형식을 지킵니다.
            - 각각의 항목에 대해 하나하나 자세히 글이 많게 작성합니다.

            ### 응답 예시:
            {
              "자기소개": "지원하는 직무와 연관성을 더 강조하면 좋습니다.",
              "기술 스택": "추가하면 좋은 기술로 SQL, Redis 등이 있습니다.",
              "경력": "프로젝트별 기여도를 더 구체적으로 작성하면 좋습니다.",
              "프로젝트": "기술적 성과를 수치로 표현하면 더 좋습니다.",
              "대외활동": "업무와 관련된 경험을 추가하면 더욱 효과적입니다."
            }

            이제 아래 이력서를 분석하고, 위 JSON 형식으로 피드백을 제공합니다.

            이력서 내용:
            """ + resume.toString();

        System.out.println("📌📌 생성된 프롬프트: \n" + prompt);
        return prompt;
    }

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

    private HttpPost createHttpPost(Map<String, Object> requestBody) throws IOException {
        String requestJson = new ObjectMapper().writeValueAsString(requestBody);

        // 요청 JSON 확인
        System.out.println("📌 OpenAI 요청 데이터: " + requestJson);


        HttpPost post = new HttpPost(apiUrl);
        post.setHeader("Authorization", "Bearer " + apiKey);
        post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(requestBody), ContentType.APPLICATION_JSON));
        return post;
    }

    private FeedbackResponseDto parseOpenAiResponse(String responseBody) {
        try {
            // 응답 JSON 확인
            System.out.println("📌📌 OpenAI 응답 데이터: " + responseBody);


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

    private Map<String, String> extractFeedback(String content) {
        if (content == null || content.trim().isEmpty()) {
            System.err.println("❌ OpenAI 응답이 비어 있습니다.");
            return getDefaultFeedback();
        }

        // 🔥 JSON 응답이면 파싱 진행
        if (content.trim().startsWith("{")) {
            System.out.println("📌📌 JSON 형식 응답 감지! → JSON 파싱 시도");
            return parseJsonFeedback(content);
        }

        System.err.println("❌ OpenAI 응답이 JSON 형식이 아닙니다.");
        return getDefaultFeedback();
    }

    private Map<String, String> parseJsonFeedback(String content) {
        Map<String, String> feedbackMap = new HashMap<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(content);

            feedbackMap.put("자기소개", rootNode.path("자기소개").asText("자기소개 없음"));
            feedbackMap.put("기술 스택", rootNode.path("기술 스택").asText("기술 스택 없음"));
            feedbackMap.put("경력", rootNode.path("경력").asText("경력 없음"));
            feedbackMap.put("프로젝트", rootNode.path("프로젝트").asText("프로젝트 없음"));
            feedbackMap.put("대외활동", rootNode.path("대외활동").asText("대외활동 없음"));

        } catch (Exception e) {
            System.err.println("❌ JSON 파싱 오류: " + e.getMessage());
            return getDefaultFeedback(); // 기본 피드백 반환
        }

        return feedbackMap;
    }

    private Map<String, String> getDefaultFeedback() {
        Map<String, String> defaultFeedback = new HashMap<>();
        defaultFeedback.put("자기소개", "데이터 없음");
        defaultFeedback.put("기술 스택", "데이터 없음");
        defaultFeedback.put("경력", "데이터 없음");
        defaultFeedback.put("프로젝트", "데이터 없음");
        defaultFeedback.put("대외활동", "데이터 없음");
        return defaultFeedback;
    }

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
