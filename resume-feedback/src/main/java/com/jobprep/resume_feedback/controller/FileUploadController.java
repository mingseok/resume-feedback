package com.jobprep.resume_feedback.controller;

import com.jobprep.resume_feedback.service.AiService;
import com.jobprep.resume_feedback.service.OcrService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public class FileUploadController {

    private final AiService aiService;
    private final OcrService ocrService;

    public FileUploadController(AiService aiService, OcrService ocrService) {
        this.aiService = aiService;
        this.ocrService = ocrService;
    }

    @PostMapping("/upload")
    public CompletableFuture<Map<String, String>> uploadFileWithFeedback(@RequestParam("file") MultipartFile file) {
        try {
            // 임시 파일 생성 및 저장
            File tempFile = File.createTempFile("upload-", ".pdf");
            file.transferTo(tempFile);

            // OCR로 텍스트 추출
            String extractedText = ocrService.extractTextFromPdfWithOcr(tempFile);

            // AI 분석 요청 실행 (비동기 호출)
            return aiService.getFeedbackForSectionsAsync(extractedText);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(Map.of("error", "파일 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
