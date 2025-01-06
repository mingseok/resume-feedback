package com.jobprep.resume_feedback.domain;

import lombok.Getter;

@Getter
public class Feedback {
    private final String basicInfo;
    private final String selfIntroduction;
    private final String technicalSkills;
    private final String workExperience;
    private final String projects;
    private final String portfolio;
    private final String activities;

    public Feedback(String basicInfo, String selfIntroduction, String technicalSkills, String workExperience,
                    String projects, String portfolio, String activities) {
        this.basicInfo = basicInfo;
        this.selfIntroduction = selfIntroduction;
        this.technicalSkills = technicalSkills;
        this.workExperience = workExperience;
        this.projects = projects;
        this.portfolio = portfolio;
        this.activities = activities;
    }
}