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

@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    @Value("${spring.ai.openai.model}")
    private String model;

//  비동기 로직
//    public CompletableFuture<FeedbackResponseDto> requestFeedback(Resume resume) {
//        return CompletableFuture.supplyAsync(() -> {
//            Map<String, Object> requestBody = buildRequestBody(resume);
//            return executeHttpRequest(requestBody);
//        });
//    }

    public FeedbackResponseDto requestFeedback(Resume resume) {
        Map<String, Object> requestBody = buildRequestBody(resume);
        return executeHttpRequest(requestBody);
    }

    private Map<String, Object> buildRequestBody(Resume resume) {
        String prompt = createPrompt(resume);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a professional resume reviewer."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 2000);
        return requestBody;
    }

    private String createPrompt(Resume resume) {
        String prompt = """
                당신은 전문적인 이력서 리뷰어입니다.
                아래 이력서를 분석하고, **반드시** JSON 형식으로만 피드백을 제공합니다.
                Chain-of-Thought(CoT) + Few-shot Learning 방식으로 상세한 피드백을 JSON 형식으로 제공합니다.

                ### 요청 형식:
                - 각 항목별로 **상세한 피드백**을 JSON 객체 형태로 작성합니다.
                - **JSON 외 다른 텍스트를 포함하지 마세요.**
                - **JSON 형식이 올바르지 않으면 요청이 실패합니다.**

                ### JSON 응답 예시 (이 형식을 따라야 합니다!):
                {
                "자기소개": "지원하는 직무와 연관성을 강조하고, 구체적인 프로젝트 경험을 추가하면 좋습니다. 예를 들어, 백엔드 개발자로 지원하는 경우 'Spring Boot 기반의 REST API 개발 경험'을 명확하게 기재하는 것이 유리합니다.", \s
                "기술 스택": "기본적인 기술 외에도 SQL과 Redis 활용 경험을 강조하면 좋습니다. 예를 들어, 'Redis를 활용한 캐싱으로 API 응답 속도를 40% 향상시킨 경험'을 기술하면 더 효과적입니다.", \s
                "경력": "각 업무별 성과를 수치로 표현하면 더 효과적입니다. 예를 들어, 'AWS 비용 최적화를 통해 인프라 비용 30% 절감'과 같은 구체적인 수치를 추가하세요.", \s
                "프로젝트": "성공한 사례를 중심으로 기술적 기여도를 강조하면 좋습니다. 예를 들어, '비동기 요청을 적용하여 AI 서빙 속도를 40% 단축'한 경험을 기재하면 강점이 부각됩니다.", \s
                "대외활동": "해당 활동이 직무에 어떤 영향을 주었는지 설명하면 좋습니다. 예를 들어, '오픈소스 프로젝트 기여 경험을 통해 코드 리뷰 및 협업 역량을 강화'한 사례를 언급하세요." \s
                }

                **중요!**
                - JSON 코드 블록 없이 **순수 JSON 데이터만 반환**하세요.
                - JSON이 아닌 응답이 나오면 요청이 실패합니다.

                이제 아래 이력서를 분석하고, 위 JSON 형식과 정확히 일치하는 JSON 응답을 반환하세요.

                이력서 내용:
                """ + resume.toString();

        return prompt;
    }

    private FeedbackResponseDto executeHttpRequest(Map<String, Object> requestBody) {
        int retryCount = 0;
        int maxRetries = 3;  // 최대 재시도 횟수

        while (retryCount < maxRetries) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost post = createHttpPost(requestBody);
                try (CloseableHttpResponse response = httpClient.execute(post)) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());

                    // JSON 파싱 시도
                    FeedbackResponseDto result = parseOpenAiResponse(responseBody);

                    // 정상적인 응답인지 확인
                    if (isValidResponse(result)) {
                        return result; // 성공하면 즉시 반환
                    } else {
                        System.err.println("❌ JSON 구조가 예상과 다름, 재시도...");
                    }
                }
            } catch (IOException e) {
                System.err.println("HTTP 요청 실패: " + e.getMessage());
            }

            retryCount++;
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(1000); // 1초 대기 후 재시도
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        System.err.println("최종적으로 JSON 파싱 실패. 기본 피드백 반환.");
        return getDefaultFeedbackResponse();
    }

    private boolean isValidResponse(FeedbackResponseDto response) {
        return response != null &&
                response.getSelfIntroduction() != null && !response.getSelfIntroduction().isEmpty() &&
                response.getTechnicalSkills() != null && !response.getTechnicalSkills().isEmpty() &&
                response.getWorkExperience() != null && !response.getWorkExperience().isEmpty() &&
                response.getProjects() != null && !response.getProjects().isEmpty() &&
                response.getActivities() != null && !response.getActivities().isEmpty();
    }

    private boolean isValidJsonResponse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(content);

            // 필수 키가 모두 존재하는지 확인
            return rootNode.has("자기소개") &&
                    rootNode.has("기술 스택") &&
                    rootNode.has("경력") &&
                    rootNode.has("프로젝트") &&
                    rootNode.has("대외활동");

        } catch (Exception e) {
            return false;
        }
    }

    private FeedbackResponseDto getDefaultFeedbackResponse() {
        return new FeedbackResponseDto(
                "자기소개 없음",
                "기술 스택 없음",
                "경력 없음",
                "프로젝트 없음",
                "대외활동 없음"
        );
    }

    private HttpPost createHttpPost(Map<String, Object> requestBody) throws IOException {
        String requestJson = new ObjectMapper().writeValueAsString(requestBody);

        // 요청 JSON 확인
        System.out.println("OpenAI 요청 데이터: " + requestJson);


        HttpPost post = new HttpPost(apiUrl);
        post.setHeader("Authorization", "Bearer " + apiKey);
        post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(requestBody), ContentType.APPLICATION_JSON));
        return post;
    }

    private FeedbackResponseDto parseOpenAiResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // JSON 응답에서 content 추출
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();

            // JSON 형식 검사
            if (content == null || !content.trim().startsWith("{")) {
                System.err.println("❌ OpenAI 응답이 JSON 형식이 아님!");
                return null; // JSON 형식이 아니면 재시도 유도
            }

            // JSON 유효성 검증 추가 (필수 키 확인)
            if (!isValidJsonResponse(content)) {
                System.err.println("❌ JSON 응답이 올바르지 않음. 재시도 진행...");
                return null; // 실패 처리하여 재시도 트리거
            }

            // JSON을 Map으로 변환 후 FeedbackResponseDto 반환
            Map<String, String> feedbackMap = parseJsonFeedback(content);
            return buildFeedbackResponse(feedbackMap);

        } catch (Exception e) {
            System.err.println("OpenAI 응답 파싱 오류: " + e.getMessage());
            return null; // JSON 파싱 실패 시 재시도 유도
        }
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
