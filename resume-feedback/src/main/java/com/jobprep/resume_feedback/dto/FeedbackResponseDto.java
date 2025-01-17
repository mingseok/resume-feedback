package com.jobprep.resume_feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponseDto {

    private String selfIntroduction;
    private String technicalSkills;
    private String workExperience;
    private String projects;
    private String activities;
}
