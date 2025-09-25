package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.exception.ResumeError;
import com.jobprep.resume_feedback.exception.ResumeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfTextExtractorService {

    private final OcrProcessor ocrProcessor;

    public String extractTextFromPdf(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            
            String extractedText = tryDirectTextExtraction(document);
            
            if (hasValidTextContent(extractedText)) {
                log.info("텍스트 기반 PDF 감지, PDFTextStripper 사용");
                return extractedText;
            }
            
            log.info("이미지 기반 PDF 감지, OCR 적용");
            return extractTextWithOcr(document);
            
        } catch (IOException e) {
            log.error("PDF 파일 처리 중 오류 발생", e);
            throw new ResumeException(ResumeError.FILE_PROCESSING_ERROR);
        }
    }

    private String tryDirectTextExtraction(PDDocument document) {
        try {
            return new PDFTextStripper().getText(document).trim();
        } catch (IOException e) {
            log.warn("직접 텍스트 추출 실패, OCR로 전환", e);
            return "";
        }
    }

    private boolean hasValidTextContent(String text) {
        return text != null && !text.trim().isEmpty() && text.length() > 10;
    }

    private String extractTextWithOcr(PDDocument document) {
        PDFRenderer renderer = new PDFRenderer(document);
        return ocrProcessor.extractTextWithOcr(renderer, document.getNumberOfPages());
    }
}
