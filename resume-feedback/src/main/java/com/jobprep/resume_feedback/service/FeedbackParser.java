package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobprep.resume_feedback.service.dto.response.FeedbackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class FeedbackParser {

    public FeedbackResponse parseResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            String content = extractContent(rootNode);
            if (content == null || !isValidJsonFormat(content)) {
                return null;
            }

            Map<String, String> feedbackMap = parseJsonFeedback(content);
            return buildFeedbackResponse(feedbackMap);

        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패", e);
            return null;
        }
    }

    private String extractContent(JsonNode rootNode) {
        JsonNode choicesNode = rootNode.path("choices");
        if (!choicesNode.isArray() || choicesNode.isEmpty()) {
            log.error("유효하지 않은 OpenAI 응답: choices 배열 누락");
            return null;
        }

        JsonNode firstChoice = choicesNode.get(0);
        if (firstChoice == null || !firstChoice.has("message")) {
            log.error("유효하지 않은 OpenAI 응답: 첫 번째 선택에 메시지 누락");
            return null;
        }

        String content = firstChoice.path("message").path("content").asText(null);
        if (content == null || content.trim().isEmpty()) {
            log.error("OpenAI 응답 내용이 비어있음");
            return null;
        }

        return content;
    }

    private boolean isValidJsonFormat(String content) {
        if (!content.trim().startsWith("{")) {
            log.error("OpenAI 응답이 JSON 형식이 아님");
            return false;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(content);
            return rootNode.has("자기소개") &&
                    rootNode.has("기술 스택") &&
                    rootNode.has("경력") &&
                    rootNode.has("프로젝트") &&
                    rootNode.has("대외활동");
        } catch (Exception e) {
            log.error("유효하지 않은 JSON 구조", e);
            return false;
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
            log.error("JSON 파싱 실패", e);
            return createDefaultFeedbackMap();
        }

        return feedbackMap;
    }

    private FeedbackResponse buildFeedbackResponse(Map<String, String> feedbackMap) {
        return FeedbackResponse.create(
                feedbackMap.getOrDefault("자기소개", "자기소개 없음"),
                feedbackMap.getOrDefault("기술 스택", "기술 스택 없음"),
                feedbackMap.getOrDefault("경력", "경력 없음"),
                feedbackMap.getOrDefault("프로젝트", "프로젝트 없음"),
                feedbackMap.getOrDefault("대외활동", "대외활동 없음")
        );
    }

    private Map<String, String> createDefaultFeedbackMap() {
        Map<String, String> defaultFeedback = new HashMap<>();
        defaultFeedback.put("자기소개", "데이터 없음");
        defaultFeedback.put("기술 스택", "데이터 없음");
        defaultFeedback.put("경력", "데이터 없음");
        defaultFeedback.put("프로젝트", "데이터 없음");
        defaultFeedback.put("대외활동", "데이터 없음");
        return defaultFeedback;
    }
}
