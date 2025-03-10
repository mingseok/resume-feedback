package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.domain.Resume;
import com.jobprep.resume_feedback.dto.FeedbackResponseDto;
import com.jobprep.resume_feedback.dto.ResumeRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeProcessingService {

    private final PdfTextExtractorService pdfTextExtractorService;
    private final ResumeParserService resumeParserService;
    private final AiFeedbackService aiFeedbackService;

//    public FeedbackResponseDto processResume(ResumeRequestDto requestDto) {
//        // PDF에서 텍스트 추출
//        String extractedText = pdfTextExtractorService.extractTextFromPdf(requestDto.getFilePath());
//        System.out.println("📌 최종 추출된 이력서 내용:\n" + extractedText);
//
//        // 추출된 텍스트를 이력서 객체로 파싱
//        Resume resume = resumeParserService.parseExtractedTextToResume(extractedText);
//        System.out.println("📌 변환된 Resume 객체: " + resume);
//
//        return aiFeedbackService.requestFeedback(resume);
//    }

    public FeedbackResponseDto processResume(ResumeRequestDto requestDto) {
        // 1️⃣ PDF에서 텍스트 추출
        String extractedText = pdfTextExtractorService.extractTextFromPdf(requestDto.getFilePath());
        System.out.println("📌 최종 추출된 이력서 내용:\n" + extractedText);

        // 2️⃣ 추출된 텍스트를 Resume 객체로 변환
        Resume resume = resumeParserService.parseExtractedTextToResume(extractedText);
        System.out.println("📌 변환된 Resume 객체:\n" + resume);

        // 3️⃣ AI 피드백 요청 (기존 프롬프트 성능 확인)
        FeedbackResponseDto response = aiFeedbackService.requestFeedback(resume);
        System.out.println("📌 기존 프롬프트로 생성된 결과:\n" + response);

        return response;
    }
}
