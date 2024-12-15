package com.jobprep.resume_feedback.controller;

import com.jobprep.resume_feedback.service.AiService;
import com.jobprep.resume_feedback.service.OcrService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Map;

@RestController
public class FileUploadController {

    private final AiService aiService;
    private final OcrService ocrService;

    public FileUploadController(AiService aiService, OcrService ocrService) {
        this.aiService = aiService;
        this.ocrService = ocrService;
    }

    @PostMapping("/upload")
    public Mono<Map<String, String>> uploadFileWithOcr(@RequestParam("file") MultipartFile file) {
        try {
            File tempFile = File.createTempFile("upload-", ".pdf");
            file.transferTo(tempFile);

            String extractedText = ocrService.extractTextFromPdfWithOcr(tempFile);

            long startTime = System.currentTimeMillis();
            System.out.println("비동기 요청 시작 시간: " + startTime);

            return aiService.getFeedbackForSectionsAsync(extractedText)
                    .doOnSuccess(response -> {
                        long endTime = System.currentTimeMillis();
                        System.out.println("비동기 요청 종료 시간: " + endTime + " (총 처리 시간: " + (endTime - startTime) + "ms)");
                    });
        } catch (Exception e) {
            return Mono.just(Map.of("error", "파일 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}