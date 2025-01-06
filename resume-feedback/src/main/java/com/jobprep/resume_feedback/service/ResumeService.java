package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobprep.resume_feedback.domain.Feedback;
import com.jobprep.resume_feedback.domain.Resume;
import com.jobprep.resume_feedback.dto.FeedbackResponseDto;
import com.jobprep.resume_feedback.dto.ResumeRequestDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Tesseract tesseract;
    private final ResourceLoader resourceLoader;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    @Value("${spring.ai.openai.model}")
    private String model;

    private SseEmitter emitter;

    private final RestTemplate restTemplate;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // SSE 구독 메서드
    public void subscribeToProgress(SseEmitter emitter) {
        executor.execute(() -> {
            try {
                for (int i = 0; i <= 100; i += 10) {
                    emitter.send("Progress: " + i + "%");
                    Thread.sleep(1000); // 진행 상황 업데이트 간격
                }
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
    }

    @PostConstruct
    public void init() {
        try {
            String tessdataPath = Paths.get(resourceLoader.getResource("classpath:tessdata").getURI()).toString();
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage("kor");
        } catch (IOException e) {
            throw new RuntimeException("TESSDATA_PREFIX 설정 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // 피드백 응답 초기화
    private FeedbackResponseDto feedbackResponseDto = new FeedbackResponseDto(
            "기본 정보 없음", "자기소개 없음", "기술 스택 없음",
            "경력 없음", "프로젝트 없음", "포트폴리오 없음", "대외활동 없음"
    );

    // 비동기 이력서 분석 처리
    public void processResumeAsync(ResumeRequestDto requestDto) {
        CompletableFuture.runAsync(() -> {
            try {
                analyzeBasicInfo(requestDto);
                updateProgress(20);

                analyzeSelfIntroduction(requestDto);
                updateProgress(40);

                analyzeTechnicalStack(requestDto);
                updateProgress(60);

                analyzeWorkExperience(requestDto);
                updateProgress(80);

                analyzeProjects(requestDto);
                updateProgress(100);

                emitter.complete();
            } catch (Exception e) {
                if (emitter != null) {
                    emitter.completeWithError(e);
                }
            }
        });
    }

    // Step 1: 기본 정보 분석
    private void analyzeBasicInfo(ResumeRequestDto requestDto) {
        List<String> fileChunks = splitFileIntoChunks(requestDto.getFileContent(), 1000);
        StringBuilder resultBuilder = new StringBuilder();

        for (String chunk : fileChunks) {
            String prompt = "Please analyze the basic information from this resume:\n" + chunk;
            String result = getOpenAiResponse(prompt);
            resultBuilder.append(result).append("\n");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        feedbackResponseDto = new FeedbackResponseDto(
                resultBuilder.toString(),
                feedbackResponseDto.getSelfIntroduction(),
                feedbackResponseDto.getTechnicalSkills(),
                feedbackResponseDto.getWorkExperience(),
                feedbackResponseDto.getProjects(),
                feedbackResponseDto.getPortfolio(),
                feedbackResponseDto.getActivities()
        );
    }

    private List<String> splitFileIntoChunks(String fileContent, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = fileContent.length();
        for (int i = 0; i < length; i += chunkSize) {
            chunks.add(fileContent.substring(i, Math.min(length, i + chunkSize)));
        }
        return chunks;
    }

    // Step 2: 자기소개 분석
    private void analyzeSelfIntroduction(ResumeRequestDto requestDto) {
        String prompt = "Please analyze the self-introduction from this resume:\n" + requestDto.getFileContent();
        String result = getOpenAiResponse(prompt);
        feedbackResponseDto = new FeedbackResponseDto(
                feedbackResponseDto.getBasicInfo(), result,
                feedbackResponseDto.getTechnicalSkills(),
                feedbackResponseDto.getWorkExperience(),
                feedbackResponseDto.getProjects(),
                feedbackResponseDto.getPortfolio(),
                feedbackResponseDto.getActivities()
        );
    }

    // Step 3: 기술 스택 분석
    private void analyzeTechnicalStack(ResumeRequestDto requestDto) {
        String prompt = "Please analyze the technical stack from this resume:\n" + requestDto.getFileContent();
        String result = getOpenAiResponse(prompt);
        feedbackResponseDto = new FeedbackResponseDto(
                feedbackResponseDto.getBasicInfo(), feedbackResponseDto.getSelfIntroduction(),
                result, feedbackResponseDto.getWorkExperience(),
                feedbackResponseDto.getProjects(),
                feedbackResponseDto.getPortfolio(),
                feedbackResponseDto.getActivities()
        );
    }

    // Step 4: 경력 분석
    private void analyzeWorkExperience(ResumeRequestDto requestDto) {
        String prompt = "Please analyze the work experience from this resume:\n" + requestDto.getFileContent();
        String result = getOpenAiResponse(prompt);
        feedbackResponseDto = new FeedbackResponseDto(
                feedbackResponseDto.getBasicInfo(), feedbackResponseDto.getSelfIntroduction(),
                feedbackResponseDto.getTechnicalSkills(), result,
                feedbackResponseDto.getProjects(),
                feedbackResponseDto.getPortfolio(),
                feedbackResponseDto.getActivities()
        );
    }

    // Step 5: 프로젝트 분석
    private void analyzeProjects(ResumeRequestDto requestDto) {
        String prompt = "Please analyze the projects from this resume:\n" + requestDto.getFileContent();
        String result = getOpenAiResponse(prompt);
        feedbackResponseDto = new FeedbackResponseDto(
                feedbackResponseDto.getBasicInfo(), feedbackResponseDto.getSelfIntroduction(),
                feedbackResponseDto.getTechnicalSkills(), feedbackResponseDto.getWorkExperience(),
                result, feedbackResponseDto.getPortfolio(),
                feedbackResponseDto.getActivities()
        );
    }

    // OpenAI API 호출 메서드
    public String getOpenAiResponse(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are an assistant that analyzes resumes."));
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        requestBody.put("max_tokens", 1000);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();
    }

    // 이력서 처리 메서드
    public void processResume(MultipartFile file) {
        // 이력서 처리 로직 (예시)
        try {
            for (int progress = 10; progress <= 100; progress += 10) {
                Thread.sleep(1000);  // 가상 처리 시간
                if (emitter != null) {
                    emitter.send(SseEmitter.event().name("progress").data(progress));
                }
            }
            if (emitter != null) {
                emitter.complete();
            }
        } catch (Exception e) {
            if (emitter != null) {
                emitter.completeWithError(e);
            }
        }
    }

    /**
     * OpenAI API에 이력서를 보내고 피드백을 요청
     */
    public Feedback requestFeedbackFromAi(Resume resume) {
        String prompt = createPrompt(resume);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(createRequestBody(prompt)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseOpenAiResponse(response.body());

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("OpenAI API 요청 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * OpenAI 프롬프트 생성
     */
    private String createPrompt(Resume resume) {
        return """
                이력서를 검토하고 다음 항목별로 피드백을 주세요 (항목당 최대 5줄로 요약):
                1. 기본 정보
                2. 자기소개
                3. 기술 스택
                4. 경력
                5. 프로젝트
                6. 포트폴리오
                7. 대외활동
                """ + resume.toString();
    }

    /**
     * OpenAI 요청 본문 생성
     */
    private String createRequestBody(String prompt) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a professional resume reviewer."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 500);

        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 요청 본문 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * OpenAI 응답을 Feedback 객체로 변환
     */
    private Feedback parseOpenAiResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            String basicInfo = rootNode.at("/choices/0/message/content/basicInfo").asText();
            String selfIntroduction = rootNode.at("/choices/0/message/content/selfIntroduction").asText();
            String technicalSkills = rootNode.at("/choices/0/message/content/technicalSkills").asText();
            String workExperience = rootNode.at("/choices/0/message/content/workExperience").asText();
            String projects = rootNode.at("/choices/0/message/content/projects").asText();
            String portfolio = rootNode.at("/choices/0/message/content/portfolio").asText();
            String activities = rootNode.at("/choices/0/message/content/activities").asText();

            return new Feedback(basicInfo, selfIntroduction, technicalSkills, workExperience, projects, portfolio, activities);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("OpenAI 응답 파싱 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * Feedback 객체를 FeedbackResponseDto로 매핑
     */
    public FeedbackResponseDto mapToResponseDto(Feedback feedback) {
        return new FeedbackResponseDto(
                feedback.getBasicInfo(),
                feedback.getSelfIntroduction(),
                feedback.getTechnicalSkills(),
                feedback.getWorkExperience(),
                feedback.getProjects(),
                feedback.getPortfolio(),
                feedback.getActivities()
        );
    }

    /**
     * 파일을 서버의 임시 디렉토리에 저장
     */
    public String saveFileToTempDir(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("resume_", "_" + file.getOriginalFilename());
        file.transferTo(tempFile);
        return tempFile.getAbsolutePath();
    }

    /**
     * PDF 파일을 텍스트로 변환하여 이력서 객체로 변환
     */
    public Resume extractResumeFromPdf(String filePath) {
        StringBuilder extractedText = new StringBuilder();

        try {
            PDDocument document = PDDocument.load(new File(filePath));
            PDFRenderer renderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 150, ImageType.RGB);
                extractedText.append(tesseract.doOCR(image));
            }
            document.close();
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("OCR 처리 중 오류 발생: " + e.getMessage(), e);
        }

        return parseExtractedTextToResume(extractedText.toString());
    }

    /**
     * 텍스트를 이력서 객체로 변환
     */
    private Resume parseExtractedTextToResume(String text) {
        String[] lines = text.split("\\n");
        String name = "Unknown";
        String contactInfo = "Unknown";
        String technicalSkills = "Unknown";
        List<String> projects = List.of();

        if (lines.length > 0) {
            name = lines[0];
        }
        if (lines.length > 1) {
            contactInfo = lines[1];
        }
        if (lines.length > 2) {
            technicalSkills = lines[2];
        }
        if (lines.length > 3) {
            projects = Arrays.asList(lines).subList(3, lines.length);
        }

        return new Resume(name, contactInfo, technicalSkills, projects);
    }

    public FeedbackResponseDto getFeedback() {
        return feedbackResponseDto;
    }

    // 진행률 업데이트 메서드
    private void updateProgress(int progress) throws Exception {
        if (emitter != null) {
            emitter.send(SseEmitter.event().name("progress").data(progress));
        }
    }
}
