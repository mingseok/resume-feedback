package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.service.dto.request.ResumeRequest;
import com.jobprep.resume_feedback.service.dto.response.FeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeProcessingService {

    private final PdfTextExtractorService pdfTextExtractorService;
    private final ResumeParserService resumeParserService;
    private final AiFeedbackService aiFeedbackService;

    public FeedbackResponse processResume(ResumeRequest request) {
        String extractedText = pdfTextExtractorService.extractTextFromPdf(request.getFile());
        Resume resume = resumeParserService.parseExtractedTextToResume(extractedText);
        return aiFeedbackService.requestFeedback(resume);
    }
}
