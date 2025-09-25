package com.jobprep.resume_feedback.service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedbackResponse {

    private final String selfIntroduction;
    private final String technicalSkills;
    private final String workExperience;
    private final String projects;
    private final String activities;

    public static FeedbackResponse create(String selfIntroduction, String technicalSkills, 
                                        String workExperience, String projects, String activities) {
        return FeedbackResponse.builder()
                .selfIntroduction(selfIntroduction)
                .technicalSkills(technicalSkills)
                .workExperience(workExperience)
                .projects(projects)
                .activities(activities)
                .build();
    }

    public boolean isEmpty() {
        return isDataMissing(selfIntroduction) &&
                isDataMissing(technicalSkills) &&
                isDataMissing(workExperience) &&
                isDataMissing(projects) &&
                isDataMissing(activities);
    }

    private boolean isDataMissing(String field) {
        return field != null && field.equals("데이터 없음");
    }
}
