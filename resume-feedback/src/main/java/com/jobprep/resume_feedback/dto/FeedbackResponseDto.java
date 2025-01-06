package com.jobprep.resume_feedback.dto;

import com.jobprep.resume_feedback.domain.Feedback;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

@Getter
@AllArgsConstructor
public class FeedbackResponseDto {
    private final String basicInfo;
    private final String selfIntroduction;
    private final String technicalSkills;
    private final String workExperience;
    private final String projects;
    private final String portfolio;
    private final String activities;
}
