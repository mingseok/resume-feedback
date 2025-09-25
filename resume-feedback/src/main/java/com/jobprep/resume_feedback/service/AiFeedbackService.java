package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.service.dto.response.FeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private final OpenAiClient openAiClient;
    private final PromptBuilder promptBuilder;
    private final FeedbackParser feedbackParser;

    public FeedbackResponse requestFeedback(Resume resume) {
        Map<String, Object> requestBody = promptBuilder.buildRequestBody(resume);
        String responseBody = openAiClient.requestCompletion(requestBody);

        FeedbackResponse result = feedbackParser.parseResponse(responseBody);

        if (isValidResponse(result)) {
            return result;
        }

        log.error("유효하지 않은 응답 수신, 기본 피드백 반환");
        return createDefaultFeedback();
    }

    private boolean isValidResponse(FeedbackResponse response) {
        return response != null &&
                hasValidContent(response.getSelfIntroduction()) &&
                hasValidContent(response.getTechnicalSkills()) &&
                hasValidContent(response.getWorkExperience()) &&
                hasValidContent(response.getProjects()) &&
                hasValidContent(response.getActivities());
    }

    private boolean hasValidContent(String content) {
        return content != null && !content.trim().isEmpty();
    }

    private FeedbackResponse createDefaultFeedback() {
        return FeedbackResponse.create(
                "자기소개 없음",
                "기술 스택 없음",
                "경력 없음",
                "프로젝트 없음",
                "대외활동 없음"
        );
    }
}
