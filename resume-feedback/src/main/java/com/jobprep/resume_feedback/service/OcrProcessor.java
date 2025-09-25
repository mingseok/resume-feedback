package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.exception.ResumeError;
import com.jobprep.resume_feedback.exception.ResumeException;
import com.jobprep.resume_feedback.util.ImageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrProcessor {

    private static final int DEFAULT_DPI = 300;

    private final Tesseract tesseract;

    public String extractTextWithOcr(PDFRenderer renderer, int pageCount) {
        long startTime = System.nanoTime();

        String extractedText = IntStream.range(0, pageCount)
                .mapToObj(page -> extractTextFromPage(renderer, page))
                .collect(Collectors.joining("\n"))
                .replaceAll("\\s+", " ")
                .trim();

        long endTime = System.nanoTime();
        double elapsedTimeMs = (endTime - startTime) / 1_000_000.0;
        log.info("OCR 완료: {:.3f}ms", elapsedTimeMs);

        return extractedText;
    }

    private String extractTextFromPage(PDFRenderer renderer, int page) {
        try {
            BufferedImage image = renderer.renderImageWithDPI(page, DEFAULT_DPI, ImageType.RGB);
            if (image == null) {
                log.warn("PDF 페이지 렌더링 실패: {}", page);
                return "";
            }
            return extractTextFromImage(image);
        } catch (Exception e) {
            log.error("페이지 {} OCR 처리 실패", page, e);
            return "";
        }
    }

    private String extractTextFromImage(BufferedImage image) {
        try {
            BufferedImage preprocessedImage = ImageProcessor.preprocessForOcr(image);
            return tesseract.doOCR(preprocessedImage).replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            throw new ResumeException(ResumeError.OCR_PROCESSING_ERROR);
        }
    }
}
