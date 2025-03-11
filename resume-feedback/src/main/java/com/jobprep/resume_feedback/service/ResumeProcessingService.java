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

    public FeedbackResponseDto processResume(ResumeRequestDto requestDto) {
        String extractedText = pdfTextExtractorService.extractTextFromPdf(requestDto.getFilePath());
        Resume resume = resumeParserService.parseExtractedTextToResume(extractedText);
        return aiFeedbackService.requestFeedback(resume);
    }
}
