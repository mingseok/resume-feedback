package com.jobprep.resume_feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiMessage {

    private String role; // 메시지 역할 (system, user, assistant)
    private String content; // 메시지 내용
}
