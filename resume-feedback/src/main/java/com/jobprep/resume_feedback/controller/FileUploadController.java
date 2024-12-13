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
    public CompletableFuture<Map<String, String>> uploadFileWithOcr(@RequestParam("file") MultipartFile file) {
        try {
            // OCR 처리
            File tempFile = File.createTempFile("upload-", ".pdf");
            file.transferTo(tempFile);
            String extractedText = ocrService.extractTextFromPdfWithOcr(tempFile);

            // 비동기 요청 처리 시작 시간
            long startTime = System.currentTimeMillis();
            System.out.println("비동기 요청 시작 시간: " + startTime);

            return aiService.getFeedbackForSectionsAsync(extractedText)
                    .thenApply(response -> {
                        long endTime = System.currentTimeMillis();
                        System.out.println("비동기 처리 총 시간: " + (endTime - startTime) + "ms"); // ## 비동기 작업 시간 로그
                        return response;
                    });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(Map.of("error", "파일 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
