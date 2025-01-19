package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.domain.Resume;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeParserService {

    /**
     * 추출된 텍스트를 이력서 객체로 변환
     *
     * @param extractedText OCR로 추출된 텍스트
     * @return Resume 변환된 이력서 객체
     */
    public Resume parseExtractedTextToResume(String extractedText) {
        String[] lines = extractedText.split("\n");
        String selfIntroduction = extractSection(lines, "자기소개", "");
        String technicalSkills = extractSection(lines, "기술 스택", "Technical Skills");
        String workExperience = extractPattern(lines, "(경력|Work Experience)");
        String activities = extractPattern(lines, "(대외활동|Activities)");
        List<String> projects = extractProjects(lines);

        return new Resume(selfIntroduction, technicalSkills, workExperience, projects, activities);
    }

    /**
     * 특정 섹션을 추출
     *
     * @param lines          텍스트 라인 배열
     * @param sectionKey     섹션 키워드
     * @param alternativeKey 대체 키워드
     * @return String 추출된 섹션 내용
     */
    private String extractSection(String[] lines, String sectionKey, String alternativeKey) {
        for (String line : lines) {
            if (line.startsWith(sectionKey) || line.toLowerCase().contains(alternativeKey.toLowerCase())) {
                return line.replace(sectionKey + ":", "").trim();
            }
        }
        return "";
    }

    /**
     * 정규식을 사용하여 섹션을 추출
     *
     * @param lines   텍스트 라인 배열
     * @param pattern 정규식 패턴
     * @return String 추출된 섹션 내용
     */
    private String extractPattern(String[] lines, String pattern) {
        Pattern compiledPattern = Pattern.compile(pattern + ":?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        for (String line : lines) {
            Matcher matcher = compiledPattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return "";
    }

    /**
     * 프로젝트 목록을 추출
     *
     * @param lines 텍스트 라인 배열
     * @return List<String> 프로젝트 목록
     */
    private List<String> extractProjects(String[] lines) {
        StringBuilder projects = new StringBuilder();
        for (String line : lines) {
            if (!line.startsWith("자기소개") && !line.startsWith("기술 스택") &&
                    !line.toLowerCase().contains("work experience") && !line.toLowerCase().contains("activities")) {
                projects.append(line).append("\n");
            }
        }
        return List.of(projects.toString().split("\n"));
    }
}
