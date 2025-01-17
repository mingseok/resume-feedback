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

    /**
     * 업로드된 이력서를 처리하고 AI 피드백을 생성합니다.
     * @param requestDto 이력서 요청 DTO
     * @return FeedbackResponseDto 피드백 응답 DTO
     */
    public FeedbackResponseDto processResume(ResumeRequestDto requestDto) {
        // PDF에서 텍스트 추출
        String extractedText = pdfTextExtractorService.extractTextFromPdf(requestDto.getFilePath());

        // 추출된 텍스트를 이력서 객체로 파싱
        Resume resume = resumeParserService.parseExtractedTextToResume(extractedText);

        // AI로부터 피드백 요청
        return aiFeedbackService.requestFeedback(resume).join();
    }
}
