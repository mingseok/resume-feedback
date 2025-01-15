package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobprep.resume_feedback.domain.Resume;
import com.jobprep.resume_feedback.dto.FeedbackResponseDto;
import com.jobprep.resume_feedback.dto.ResumeRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final RestTemplate restTemplate;
    private final OcrService ocrService;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    @Value("${spring.ai.openai.model}")
    private String model;

    private FeedbackResponseDto feedbackResponseDto;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // SSE 구독 메서드
    public void subscribeToProgress(SseEmitter emitter) {
        executorService.submit(() -> {
            try {
                for (int i = 0; i <= 100; i += 10) {
                    emitter.send(SseEmitter.event().name("progress").data(i));
                    Thread.sleep(500);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    public void processResume(ResumeRequestDto requestDto) {
        try {
            Resume resume = extractResumeFromFile(requestDto);  // 이력서 정보 추출
            FeedbackResponseDto feedback = requestFeedbackFromAi(resume);  // OpenAI API 요청

            // 디버깅 출력
            System.out.println("🔧 받은 피드백: " + feedback);

            setFeedback(feedback);  // 피드백 저장
        } catch (Exception e) {
            e.printStackTrace();
            setFeedback(new FeedbackResponseDto(
                    "자기소개 없음", "기술 스택 없음",
                    "경력 없음", "프로젝트 없음", "대외활동 없음"
            ));
        }
    }

    private Resume extractResumeFromFile(ResumeRequestDto requestDto) {
        String filePath = requestDto.getFilePath();  // 파일 경로로 변경
        String extractedText = ocrService.extractTextFromPdf(filePath);  // OCR로 텍스트 추출
        return parseExtractedTextToResume(extractedText);  // 추출된 텍스트를 Resume 객체로 변환
    }

    private Resume parseExtractedTextToResume(String extractedText) {
        // 텍스트 줄 단위로 나누기
        String[] lines = extractedText.split("\\n");

        String selfIntroduction = "";
        String technicalSkills = "";
        String workExperience = "";
        List<String> projects = new ArrayList<>();
        String activities = "";

        // 정규식 패턴 설정
        Pattern workExperiencePattern = Pattern.compile("(경력|Work Experience):?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Pattern activitiesPattern = Pattern.compile("(대외활동|Activities):?\\s*(.*)", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("자기소개")) {
                selfIntroduction = line.replace("자기소개:", "").trim();
            } else if (line.startsWith("기술 스택") || line.toLowerCase().contains("technical skills")) {
                technicalSkills = line.replace("기술 스택:", "").trim();
            } else if (workExperiencePattern.matcher(line).find()) {
                Matcher matcher = workExperiencePattern.matcher(line);
                if (matcher.find()) {
                    workExperience = matcher.group(2).trim();
                }
            } else if (activitiesPattern.matcher(line).find()) {
                Matcher matcher = activitiesPattern.matcher(line);
                if (matcher.find()) {
                    activities = matcher.group(2).trim();
                }
            } else {
                // 프로젝트 정보 추가
                projects.add(line);
            }
        }

        return new Resume(selfIntroduction, technicalSkills, workExperience, projects, activities);
    }

    public FeedbackResponseDto requestFeedbackFromAi(Resume resume) {
        String prompt = createPrompt(resume);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You are a professional resume reviewer."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 1000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 동기 호출
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            // 🛠 디버깅 코드 추가
            System.out.println("📝 요청한 프롬프트: " + prompt);
            System.out.println("📩 OpenAI API 응답: " + response.getBody());

            return parseOpenAiResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            System.err.println("HTTP 오류: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API 요청 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API 요청 중 예기치 못한 오류 발생: " + e.getMessage(), e);
        }
    }

    private String createPrompt(Resume resume) {
        return """
        이력서를 검토하고 다음 항목별로 피드백을 주세요. 항목당 8줄까지 작성해줘.:
        1. 자기소개
        2. 기술 스택
        3. 경력
        4. 프로젝트
        5. 대외활동
        """ + resume.toString();
    }

    private FeedbackResponseDto parseOpenAiResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // 응답에서 choices 배열 확인
            if (!rootNode.has("choices") || rootNode.get("choices").isEmpty()) {
                throw new RuntimeException("OpenAI 응답에 choices 항목이 없습니다.");
            }

            // 응답에서 content 추출
            String content = rootNode.at("/choices/0/message/content").asText();

            // 🔧 디버깅 출력
            System.out.println("📩 API 응답 내용: " + content);

            // 카테고리별로 줄바꿈과 콜론(:)을 기준으로 매칭
            Map<String, String> feedbackMap = new HashMap<>();
            Pattern pattern = Pattern.compile("(?m)(자기소개|기술 스택|경력|프로젝트|대외활동):\\s*(.*)");
            Matcher matcher = pattern.matcher(content);

            // 매칭된 카테고리를 feedbackMap에 저장
            while (matcher.find()) {
                String category = matcher.group(1).trim();
                String feedback = matcher.group(2).trim();
                feedbackMap.put(category, feedback);
            }

            // 🔧 디버깅 출력
            System.out.println("🔧 파싱된 피드백 내용: " + feedbackMap);

            // FeedbackResponseDto 생성
            return new FeedbackResponseDto(
                    feedbackMap.getOrDefault("자기소개", "자기소개 없음"),
                    feedbackMap.getOrDefault("기술 스택", "기술 스택 없음"),
                    feedbackMap.getOrDefault("경력", "경력 없음"),
                    feedbackMap.getOrDefault("프로젝트", "프로젝트 없음"),
                    feedbackMap.getOrDefault("대외활동", "대외활동 없음")
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("OpenAI 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public FeedbackResponseDto getFeedback() {
        System.out.println("📋 저장된 피드백 반환: " + feedbackResponseDto);
        if (feedbackResponseDto == null) {
            return new FeedbackResponseDto(
                    "자기소개 없음", "기술 스택 없음",
                    "경력 없음", "프로젝트 없음", "대외활동 없음"
            );
        }
        return feedbackResponseDto;
    }

    public void setFeedback(FeedbackResponseDto feedbackResponseDto) {
        System.out.println("🔍 피드백 저장: " + feedbackResponseDto);
        this.feedbackResponseDto = feedbackResponseDto;
    }
}
