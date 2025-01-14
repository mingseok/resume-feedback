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

    // 섹션별 데이터 추출 메서드
    private String extractSection(String content, String sectionTitle) {
        String[] lines = content.split("\n");
        StringBuilder sectionContent = new StringBuilder();

        boolean sectionFound = false;
        for (String line : lines) {
            if (line.contains(sectionTitle)) {
                sectionFound = true;
            } else if (sectionFound && line.isEmpty()) {
                break;
            } else if (sectionFound) {
                sectionContent.append(line).append(" ");
            }
        }

        return sectionContent.toString().trim();
    }


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
        // 텍스트 줄별로 나누기
        String[] lines = extractedText.split("\\n");

        // 기본 값 설정
        String selfIntroduction = "자기소개 없음";
        String technicalSkills = "기술스택 없음";
        String workExperience = "경력 없음";
        List<String> projects = new ArrayList<>();
        String activities = "대외활동 없음";

        // 줄별로 이력서 정보를 추출
        if (lines.length > 0) {
            selfIntroduction = lines[0].trim();
        }
        if (lines.length > 1) {
            technicalSkills = lines[1].trim();
        }
        if (lines.length > 2) {
            workExperience = lines[2].trim();
        }
        if (lines.length > 3) {
            // 네 번째 줄부터는 프로젝트 정보로 간주
            for (int i = 3; i < lines.length; i++) {
                projects.add(lines[i].trim());
            }
        }
        if (lines.length > 4) {
            workExperience = lines[4].trim();
        }

        // Resume 객체로 반환
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
        이력서를 검토하고 다음 항목별로 피드백을 주세요 (항목당 최대 5줄로 요약):
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

            // 응답에서 content 추출
            String content = rootNode.at("/choices/0/message/content").asText();

            // 🔧 카테고리별 피드백 추출
            Map<String, String> feedbackMap = new HashMap<>();
            Pattern pattern = Pattern.compile("(?m)^\\d+\\.\\s*(.+?):\\s*(.*)$");
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String category = matcher.group(1).trim();
                String feedback = matcher.group(2).trim();
                feedbackMap.put(category, feedback);
            }

            // 🔧 디버깅 출력
            System.out.println("🔧 파싱된 피드백 내용: " + feedbackMap);

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


    private Map<String, String> extractFeedbackByCategory(String content) {
        Map<String, String> feedbackMap = new HashMap<>();
        String[] lines = content.split("\n");

        String currentCategory = null;
        StringBuilder feedbackBuilder = new StringBuilder();

        for (String line : lines) {
            // 카테고리 번호로 시작하는 라인을 구분하여 카테고리 설정
            if (line.matches("^\\d+\\..*")) {
                // 기존 카테고리의 피드백을 저장
                if (currentCategory != null) {
                    feedbackMap.put(currentCategory.trim(), feedbackBuilder.toString().trim());
                }
                // 새로운 카테고리 시작
                switch (line.split("\\.", 2)[0].trim()) {
                    case "1":
                        currentCategory = "자기소개";
                        break;
                    case "2":
                        currentCategory = "기술 스택";
                        break;
                    case "3":
                        currentCategory = "경력";
                        break;
                    case "4":
                        currentCategory = "프로젝트";
                        break;
                    case "5":
                        currentCategory = "대외활동";
                        break;
                    default:
                        currentCategory = null;
                }
                feedbackBuilder.setLength(0);  // StringBuilder 초기화
            } else if (currentCategory != null) {
                feedbackBuilder.append(line).append(" ");
            }
        }

        // 마지막 카테고리 저장
        if (currentCategory != null) {
            feedbackMap.put(currentCategory.trim(), feedbackBuilder.toString().trim());
        }

        // 🔧 디버깅 코드
        System.out.println("🔧 파싱된 피드백 키 목록: " + feedbackMap.keySet());
        System.out.println("🔧 파싱된 피드백 내용: " + feedbackMap);

        return feedbackMap;
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

    private FeedbackResponseDto parseFeedbackResponse(String content) {
        String[] lines = content.split("\n");

        String selfIntroduction = "";
        String technicalSkills = "";
        String workExperience = "";
        String projects = "";
        String activities = "";

        for (String line : lines) {
            if (line.startsWith("1.")) {
                selfIntroduction = line.substring(3).trim();
            } else if (line.startsWith("2.")) {
                technicalSkills = line.substring(3).trim();
            } else if (line.startsWith("3.")) {
                workExperience = line.substring(3).trim();
            } else if (line.startsWith("4.")) {
                projects = line.substring(3).trim();
            } else if (line.startsWith("5.")) {
                activities = line.substring(3).trim();
            }
        }

        return new FeedbackResponseDto(selfIntroduction, technicalSkills, workExperience, projects, activities);
    }

    public String getExtractedPdfText(ResumeRequestDto requestDto) {
        try {
            String pdfFilePath = requestDto.getFilePath();  // 업로드된 PDF 경로
            return ocrService.extractTextFromPdf(pdfFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            return "PDF 내용을 불러오는 중 오류가 발생했습니다.";
        }
    }
}
