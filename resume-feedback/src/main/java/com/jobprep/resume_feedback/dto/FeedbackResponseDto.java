package com.jobprep.resume_feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponseDto {

    private String selfIntroduction;
    private String technicalSkills;
    private String workExperience;
    private String projects;
    private String activities;
}
