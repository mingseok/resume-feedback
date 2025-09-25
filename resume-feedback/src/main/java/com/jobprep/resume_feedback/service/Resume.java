package com.jobprep.resume_feedback.service;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class Resume {

    private final String selfIntroduction;
    private final String technicalSkills;
    private final String workExperience;
    private final List<String> projects;
    private final String activities;

    public static Resume create(String selfIntroduction, String technicalSkills, 
                              String workExperience, List<String> projects, String activities) {
        return Resume.builder()
                .selfIntroduction(selfIntroduction)
                .technicalSkills(technicalSkills)
                .workExperience(workExperience)
                .projects(projects)
                .activities(activities)
                .build();
    }

    public String getProjectsAsString() {
        return projects != null ? String.join(", ", projects) : "";
    }

    @Override
    public String toString() {
        return "자기소개: " + selfIntroduction + "\n"
                + "기술 스택: " + technicalSkills + "\n"
                + "경력: " + workExperience + "\n"
                + "프로젝트: " + getProjectsAsString() + "\n"
                + "대외활동: " + activities;
    }
}
