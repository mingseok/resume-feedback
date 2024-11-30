package com.jobprep.resume_feedback.controller;

import com.jobprep.resume_feedback.service.OcrService;
import com.jobprep.resume_feedback.service.OpenAiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

@RestController
public class FileUploadController {

    private final OpenAiService openAiService;
    private final OcrService ocrService;

    public FileUploadController(OpenAiService openAiService, OcrService ocrService) {
        this.openAiService = openAiService;
        this.ocrService = ocrService;
    }

    @PostMapping("/upload")
    public Map<String, String> uploadFileWithOcr(@RequestParam("file") MultipartFile file) {
        long startTime = System.currentTimeMillis(); // 요청 시작 시간
        StringBuilder logSummary = new StringBuilder(); // 간소화된 로그 저장

        try {
            logSummary.append("\n========= OCR 응답 시간 측정 시작 =========\n");

            // 1. 원본 PDF 파일 크기 가져오기
            long pdfFileSize = file.getSize(); // 바이트 단위 크기
            logSummary.append(String.format("PDF 파일 크기: %d bytes\n", pdfFileSize));

            // 2. OCR로 텍스트 추출
            long ocrStartTime = System.currentTimeMillis();
            File tempFile = File.createTempFile("upload-", ".pdf");
            file.transferTo(tempFile);
            String extractedText = ocrService.extractTextFromPdfWithOcr(tempFile);
            long ocrEndTime = System.currentTimeMillis(); // OCR 완료 시간
            logSummary.append(String.format("OCR 처리 시간: %d ms\n", ocrEndTime - ocrStartTime));

            // 3. 추출된 텍스트 크기 가져오기
            int textLength = extractedText.length();
            logSummary.append(String.format("추출된 텍스트 크기: %d characters\n", textLength));

            // 4. OpenAI API 호출
            long apiStartTime = System.currentTimeMillis();
            Map<String, String> feedback = openAiService.getFeedbackForSections(extractedText, logSummary);
            long apiEndTime = System.currentTimeMillis();
            logSummary.append(String.format("OpenAI API 전체 처리 시간: %d ms\n", apiEndTime - apiStartTime));

            // 5. 총 처리 시간 계산
            long endTime = System.currentTimeMillis();
            logSummary.append(String.format("총 처리 시간: %d ms\n", endTime - startTime));
            logSummary.append("========= OCR 응답 시간 측정 종료 =========");

            return feedback;
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis();
            logSummary.append(String.format("오류 발생, 총 처리 시간: %d ms\n", errorTime - startTime));
            logSummary.append(String.format("오류 메시지: %s\n", e.getMessage()));
            return Map.of("error", "파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
