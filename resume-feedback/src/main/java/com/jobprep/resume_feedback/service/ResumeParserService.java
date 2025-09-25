package com.jobprep.resume_feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeParserService {

    private final TextSectionExtractor textSectionExtractor;

    public Resume parseExtractedTextToResume(String extractedText) {
        String cleanedText = cleanExtractedText(extractedText);
        String[] lines = cleanedText.split("\n");
        
        return buildResume(lines);
    }

    private String cleanExtractedText(String text) {
        return text.replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .replaceAll("\\s{2,}", " ")
                .replaceAll("(?i)(데이터 통해 확인\\s*){2,}", "데이터 통해 확인")
                .trim();
    }

    private Resume buildResume(String[] lines) {
        String selfIntroduction = textSectionExtractor.extractSection(lines, "자기소개");
        String technicalSkills = textSectionExtractor.extractSection(lines, "기술 스택");
        String workExperience = textSectionExtractor.extractSection(lines, "경력");
        String activities = textSectionExtractor.extractSection(lines, "대외활동");
        List<String> projects = textSectionExtractor.extractProjects(lines);

        return Resume.create(selfIntroduction, technicalSkills, workExperience, projects, activities);
    }
}
