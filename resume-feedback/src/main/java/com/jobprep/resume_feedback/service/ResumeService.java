package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobprep.resume_feedback.domain.Resume;
import com.jobprep.resume_feedback.dto.FeedbackResponseDto;
import com.jobprep.resume_feedback.dto.ResumeRequestDto;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final OcrService ocrService;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    @Value("${spring.ai.openai.model}")
    private String model;

    public FeedbackResponseDto processResume(ResumeRequestDto requestDto) {
        try {
            Resume resume = extractResumeFromFile(requestDto); // OCR로 텍스트 추출
            FeedbackResponseDto feedback = requestFeedbackFromAiAsync(resume).join(); // OpenAI 호출
            System.out.println("🔍 생성된 FeedbackResponseDto: " + feedback);
            return feedback;
        } catch (Exception e) {
            e.printStackTrace();
            return new FeedbackResponseDto(
                    "자기소개 없음", "기술 스택 없음",
                    "경력 없음", "프로젝트 없음", "대외활동 없음"
            );
        }
    }

    private Resume extractResumeFromFile(ResumeRequestDto requestDto) {
        String filePath = requestDto.getFilePath(); // 파일 경로 가져오기
        String extractedText = ocrService.extractTextFromPdf(filePath); // OCR로 텍스트 추출
        return parseExtractedTextToResume(extractedText); // 텍스트를 Resume 객체로 변환
    }

    private Resume parseExtractedTextToResume(String extractedText) {
        String[] lines = extractedText.split("\\n");

        String selfIntroduction = "";
        String technicalSkills = "";
        String workExperience = "";
        String activities = "";
        StringBuilder projects = new StringBuilder();

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
                projects.append(line).append("\n");
            }
        }

        return new Resume(selfIntroduction, technicalSkills, workExperience, List.of(projects.toString()), activities);
    }

    public CompletableFuture<FeedbackResponseDto> requestFeedbackFromAiAsync(Resume resume) {
        return CompletableFuture.supplyAsync(() -> {
            String prompt = createPrompt(resume);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You are a professional resume reviewer."),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 1000);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(apiUrl);
                post.setHeader("Authorization", "Bearer " + apiKey);
                post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
                post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(requestBody), ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = httpClient.execute(post)) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return parseOpenAiResponse(responseBody);
                }
            } catch (IOException e) {
                throw new RuntimeException("HTTP 요청 실패: " + e.getMessage(), e);
            }
        });
    }

    private String createPrompt(Resume resume) {
        return """
    아래는 이력서 검토 요청입니다. 각 항목에 대해 간결하고 구체적인 피드백을 주세요. 각 항목은 15줄 이내로 작성해주세요:

    1. 자기소개: 본인의 강점과 역할을 중심으로 소개 내용을 작성해주세요.
    2. 기술 스택: 사용한 기술이 어떤지 설명해주세요.
    3. 경력: 과거 직무에서 수행한 역할과 성과를 중심으로 작성해주세요.
    4. 프로젝트: 수행한 프로젝트의 주요 내용, 본인의 기여도 및 성과를 설명해주세요.
    5. 대외활동: 참여한 활동과 이를 통해 얻은 경험 및 성장 내용을 작성해주세요.

    이력서 내용:
    """ + resume.toString();
    }

    private FeedbackResponseDto parseOpenAiResponse(String responseBody) {
        try {
            // JSON 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // OpenAI 응답에서 content 추출
            String content = rootNode
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            System.out.println("📩 OpenAI 응답 내용: " + content);

            // 정규식을 사용해 항목별 데이터 추출
            Map<String, String> feedbackMap = new HashMap<>();
            Pattern pattern = Pattern.compile("(?m)^([가-힣a-zA-Z ]+):\\s*(.*?)(?=^[가-힣a-zA-Z ]+:|$)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String key = matcher.group(1).trim(); // 항목 이름
                String value = matcher.group(2).trim(); // 항목 내용
                feedbackMap.put(key, value);
                System.out.println("🔧 매핑된 키: " + key);
                System.out.println("🔧 매핑된 값: " + value);
            }

            // FeedbackResponseDto 생성
            return new FeedbackResponseDto(
                    feedbackMap.getOrDefault("자기소개", "자기소개 없음"),
                    feedbackMap.getOrDefault("기술 스택", "기술 스택 없음"),
                    feedbackMap.getOrDefault("경력", "경력 없음"),
                    feedbackMap.getOrDefault("프로젝트", "프로젝트 없음"),
                    feedbackMap.getOrDefault("대외활동", "대외활동 없음")
            );

        } catch (Exception e) {
            throw new RuntimeException("OpenAI 응답 파싱 오류: " + e.getMessage(), e);
        }
    }
}
