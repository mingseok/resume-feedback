package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.domain.Resume;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ResumeParserService {

    public Resume parseExtractedTextToResume(String extractedText) {
        extractedText = extractedText.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        extractedText = cleanExtractedText(extractedText);
        String[] lines = extractedText.split("\n");
        Set<String> sectionTitles = Set.of("자기소개", "기술 스택", "경력", "대외활동", "프로젝트");
        String selfIntroduction = extractSection(lines, "자기소개", sectionTitles);
        String technicalSkills = extractSection(lines, "기술 스택", sectionTitles);
        String workExperience = extractSection(lines, "경력", sectionTitles);
        String activities = extractSection(lines, "대외활동", sectionTitles);
        List<String> projects = extractProjects(lines);
        return new Resume(selfIntroduction, technicalSkills, workExperience, projects, activities);
    }

    private String extractSection(String[] lines, String sectionName, Set<String> allSections) {
        StringBuilder sectionContent = new StringBuilder();
        boolean isSectionFound = false;

        for (String line : lines) {
            if (line.trim().equalsIgnoreCase(sectionName)) {
                isSectionFound = true;
                continue;
            }

            if (isSectionFound) {
                if (allSections.contains(line.trim())) break;
                sectionContent.append(line).append(" ");
            }
        }
        return sectionContent.toString().trim();
    }

    private String cleanExtractedText(String text) {
        return text.replaceAll("\\s{2,}", " ") // 여러 공백을 하나로
                .replaceAll("(?i)(데이터 통해 확인\\s*){2,}", "데이터 통해 확인") // 중복된 문구 제거
                .trim();
    }

    private List<String> extractProjects(String[] lines) {
        List<String> projects = new ArrayList<>();
        boolean foundProjects = false;
        StringBuilder currentProject = new StringBuilder();

        for (String line : lines) {
            if (line.trim().equalsIgnoreCase("포트폴리오")) break; // 프로젝트 종료
            if (line.trim().equalsIgnoreCase("프로젝트")) {
                foundProjects = true;
                continue;
            }

            if (foundProjects) {
                if (line.trim().isEmpty()) {
                    // 새 프로젝트 시작되면 추가하고 초기화
                    if (!currentProject.toString().isEmpty()) {
                        projects.add(currentProject.toString().trim());
                        currentProject.setLength(0);
                    }
                } else {
                    currentProject.append(line).append("\n");
                }
            }
        }

        if (!currentProject.toString().isEmpty()) {
            projects.add(currentProject.toString().trim());
        }

        return projects;
    }
}
