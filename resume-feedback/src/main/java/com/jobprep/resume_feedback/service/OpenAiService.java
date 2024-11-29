package com.jobprep.resume_feedback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAiService {

    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    public OpenAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, String> getFeedbackForSections(String content, StringBuilder logSummary) {
        String[] questions = {
                "이력서의 기본 정보(이름, 연락처, 이메일 등)가 적절하게 포함되고 형식이 올바른지 평가해주세요.",
                "기술 스택이 직무에 적합하고 충분히 설명되었는지 평가해주세요. 주요 기술에 대한 이해를 보여주는지 확인해주세요.",
                "경력 사항과 포트폴리오가 직무와 연관성이 높고 주요 성과가 잘 드러나 있는지 평가해주세요.",
                "대외활동과 자격증이 직무와 관련성이 있으며, 지원자의 역량을 보완하는지 평가해주세요.",
                "자기소개서가 직무와 적합하고, 지원자의 강점과 가치관을 잘 전달하고 있는지 평가해주세요."
        };

        Map<String, String> feedbackMap = new HashMap<>();
        long totalApiTime = 0;

        for (String question : questions) {
            long startTime = System.currentTimeMillis(); // API 호출 시작 시간
            String response = callOpenAiApi(question, content);
            long endTime = System.currentTimeMillis(); // API 호출 종료 시간

            // 질문에 따라 로그 저장
            if (question.contains("기본 정보")) {
                logSummary.append(String.format("기본 정보 피드백 응답 시간: %d ms\n", endTime - startTime));
            } else if (question.contains("기술 스택")) {
                logSummary.append(String.format("기술 스택 피드백 응답 시간: %d ms\n", endTime - startTime));
            } else if (question.contains("경력 사항과 포트폴리오")) {
                logSummary.append(String.format("경력 사항 피드백 응답 시간: %d ms\n", endTime - startTime));
            } else if (question.contains("대외활동과 자격증")) {
                logSummary.append(String.format("대외활동, 자격증 피드백 응답 시간: %d ms\n", endTime - startTime));
            } else if (question.contains("자기소개서")) {
                logSummary.append(String.format("자기소개서 피드백 응답 시간: %d ms\n", endTime - startTime));
            }

            feedbackMap.put(question, response);
            totalApiTime += (endTime - startTime);
        }

        // OpenAI API 전체 처리 시간 출력
        return feedbackMap;
    }

    private String callOpenAiApi(String question, String content) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", new Object[]{
                Map.of("role", "system", "content", "You are a helpful assistant."),
                Map.of("role", "user", "content", question + "\n\n이력서 내용:\n" + content)
        });

        // Authorization 헤더 추가
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            System.out.println("OpenAI API 응답: " + response.getBody()); // 로그 추가
            return extractMessageFromResponse(response.getBody());
        } catch (Exception e) {
            System.out.println("OpenAI API 호출 중 오류: " + e.getMessage()); // 로그 추가
            return "API 호출 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String extractMessageFromResponse(Map<String, Object> response) {
        if (response == null || !response.containsKey("choices")) {
            return "API 응답이 비어있습니다.";
        }

        var choices = (Iterable<Map<String, Object>>) response.get("choices");
        for (Map<String, Object> choice : choices) {
            Map<String, String> message = (Map<String, String>) choice.get("message");
            if (message != null && message.containsKey("content")) {
                return message.get("content");
            }
        }
        return "API 응답에서 메시지를 찾을 수 없습니다.";
    }
}
