package com.jobprep.resume_feedback.domain;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class Resume {
    private final String name;
    private final String contactInfo;
    private final String technicalSkills;
    private final List<String> projects;

    public Resume(String name, String contactInfo, String technicalSkills, List<String> projects) {
        this.name = name;
        this.contactInfo = contactInfo;
        this.technicalSkills = technicalSkills;
        this.projects = projects;
    }
}
