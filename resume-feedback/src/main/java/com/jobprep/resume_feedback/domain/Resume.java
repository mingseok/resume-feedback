package com.jobprep.resume_feedback.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Resume {

    private String selfIntroduction; // 자기소개
    private String technicalSkills;  // 기술 스택
    private String workExperience;   // 경력
    private List<String> projects;   // 프로젝트
    private String activities;       // 대외활동

    @Override
    public String toString() {
        return "자기소개: " + selfIntroduction + "\n"
                + "기술 스택: " + technicalSkills + "\n"
                + "경력: " + workExperience + "\n"
                + "프로젝트: " + String.join(", ", projects) + "\n"
                + "대외활동: " + activities;
    }
}
