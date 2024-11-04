package com.jobprep.resume_feedback.controller;

import com.jobprep.resume_feedback.service.OcrService;
import com.jobprep.resume_feedback.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class FileUploadController {

    private final OpenAiService openAiService;
    private final OcrService ocrService;

    @GetMapping("/")
    public String showHome() {
        return "index";
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            // 이력서 내용 추출
            String fileType = file.getContentType();
            String resumeContent = null;

            if ("text/plain".equals(fileType)) {
                resumeContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else if ("application/pdf".equals(fileType)) {
                File tempFile = File.createTempFile("uploaded-", "." + file.getOriginalFilename());
                file.transferTo(tempFile);
                resumeContent = ocrService.extractTextFromPdfWithOcr(tempFile);
                tempFile.delete();  // 임시 파일 삭제
            }

            if (resumeContent == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "지원하지 않는 파일 형식입니다"));
            }

            // 피드백 요청 및 결과 파싱
            String initialFeedback = openAiService.getDetailedFeedback(resumeContent);
            Map<String, String> categorizedFeedback = openAiService.parseFeedbackByCategory(initialFeedback);

            // 각 항목별로 추가 평가 요청 및 결과 저장
            Map<String, String> finalDetailedFeedback = new HashMap<>();
            for (Map.Entry<String, String> entry : categorizedFeedback.entrySet()) {
                String category = entry.getKey();
                String feedback = entry.getValue();
                String detailedFeedback = openAiService.requestFurtherEvaluation(category, feedback);
                finalDetailedFeedback.put(category, detailedFeedback);
            }

            return ResponseEntity.ok(finalDetailedFeedback);

        } catch (IOException | TesseractException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "파일 처리 실패"));
        }
    }
}
