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

    public boolean isEmpty() {
        return selfIntroduction.equals("데이터 없음") &&
                technicalSkills.equals("데이터 없음") &&
                workExperience.equals("데이터 없음") &&
                projects.equals("데이터 없음") &&
                activities.equals("데이터 없음");
    }
}
