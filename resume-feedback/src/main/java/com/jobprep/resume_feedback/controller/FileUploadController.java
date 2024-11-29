package com.jobprep.resume_feedback.controller;

import com.jobprep.resume_feedback.service.OpenAiService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class FileUploadController {

    private final OpenAiService openAiService;

    public FileUploadController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping("/upload")
    public Map<String, String> uploadFileWithDetailedQuestions(@RequestParam("file") MultipartFile file) {
        long startTime = System.currentTimeMillis(); // 요청 시작 시간
        StringBuilder logSummary = new StringBuilder(); // 간소화된 로그 저장

        try {
            logSummary.append("\n========= 응답 시간 측정 시작 =========\n");

            // 1. 원본 PDF 파일 크기 가져오기
            long pdfFileSize = file.getSize(); // 바이트 단위 크기
            logSummary.append(String.format("PDF 파일 크기: %d bytes\n", pdfFileSize));

            // 2. PDF 텍스트 추출
            long pdfStartTime = System.currentTimeMillis();
            String pdfContent = extractTextFromPdf(file); // PDF 텍스트 추출
            long pdfEndTime = System.currentTimeMillis(); // PDF 추출 완료 시간
            logSummary.append(String.format("PDF 처리 시간: %d ms\n", pdfEndTime - pdfStartTime));

            // 3. 추출된 텍스트 크기 가져오기
            int textLength = pdfContent.length(); // 문자 수
            byte[] textBytes = pdfContent.getBytes("UTF-8");
            int textByteSize = textBytes.length;
            logSummary.append(String.format("추출된 텍스트 크기: %d bytes\n", textByteSize));

            // 4. OpenAI API 호출
            long apiStartTime = System.currentTimeMillis();
            Map<String, String> feedback = openAiService.getFeedbackForSections(pdfContent, logSummary);
            long apiEndTime = System.currentTimeMillis();
            logSummary.append(String.format("OpenAI API 전체 처리 시간: %d ms\n", apiEndTime - apiStartTime));

            // 5. 총 처리 시간 계산
            long endTime = System.currentTimeMillis();
            logSummary.append(String.format("총 처리 시간: %d ms\n", endTime - startTime));
            logSummary.append("========= 응답 시간 측정 종료 =========");

            // 6. 로그 출력
            System.out.println(logSummary.toString());

            return feedback;
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis();
            logSummary.append(String.format("오류 발생, 총 처리 시간: %d ms\n", errorTime - startTime));
            logSummary.append(String.format("오류 메시지: %s\n", e.getMessage()));
            System.out.println(logSummary.toString());
            return Map.of("error", "파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }
}
