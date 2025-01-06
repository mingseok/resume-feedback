package com.jobprep.resume_feedback.dto;

import lombok.Getter;

@Getter
public class QuestionResponse {
    private final String question;
    private final String response;

    public QuestionResponse(String question, String response) {
        this.question = question;
        this.response = response;
    }
}
