package com.jobprep.resume_feedback.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpenAiRequest {

    private String model; // 모델 이름 (예: gpt-3.5-turbo)
    private List<OpenAiMessage> messages; // 대화형 메시지 리스트
    private int max_tokens; // 최대 토큰 수
}
