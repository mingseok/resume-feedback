package com.jobprep.resume_feedback.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class TextSectionExtractor {

    private static final Set<String> SECTION_TITLES = Set.of("자기소개", "기술 스택", "경력", "대외활동", "프로젝트");

    public String extractSection(String[] lines, String sectionName) {
        StringBuilder sectionContent = new StringBuilder();
        boolean isSectionFound = false;

        for (String line : lines) {
            if (line.trim().equalsIgnoreCase(sectionName)) {
                isSectionFound = true;
                continue;
            }

            if (isSectionFound) {
                if (SECTION_TITLES.contains(line.trim())) {
                    break;
                }
                sectionContent.append(line).append(" ");
            }
        }
        return sectionContent.toString().trim();
    }

    public List<String> extractProjects(String[] lines) {
        List<String> projects = new ArrayList<>();
        boolean foundProjects = false;
        StringBuilder currentProject = new StringBuilder();

        for (String line : lines) {
            if (line.trim().equalsIgnoreCase("포트폴리오")) {
                break;
            }
            
            if (line.trim().equalsIgnoreCase("프로젝트")) {
                foundProjects = true;
                continue;
            }

            if (foundProjects) {
                if (line.trim().isEmpty()) {
                    addCurrentProject(projects, currentProject);
                } else {
                    currentProject.append(line).append("\n");
                }
            }
        }

        addCurrentProject(projects, currentProject);
        return projects;
    }

    private void addCurrentProject(List<String> projects, StringBuilder currentProject) {
        if (currentProject.length() > 0) {
            projects.add(currentProject.toString().trim());
            currentProject.setLength(0);
        }
    }
}
