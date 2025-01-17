package com.jobprep.resume_feedback.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class OcrService {
    public static final int DPI = 370;
    private final ResourceLoader resourceLoader;
    private final Tesseract tesseract;

    @PostConstruct
    public void init() {
        try {
            String tessdataPath = Paths.get(resourceLoader.getResource("classpath:tessdata").getURI()).toString();
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage("kor+eng");  // 한국어와 영어 동시 인식
            tesseract.setPageSegMode(3);  // PSM 설정 (기본 페이지 세분화 모드)
        } catch (IOException e) {
            throw new RuntimeException("TESSDATA_PREFIX 설정 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 텍스트 정규화 메서드
     * - 불필요한 공백, 특수문자 제거
     */
    private String normalizeText(String text) {
        return text.replaceAll("[^가-힣a-zA-Z0-9\\s]", "")  // 한글, 영어, 숫자, 공백만 남김
                .replaceAll("\\s{2,}", " ")              // 중복 공백 제거
                .trim();
    }

    /**
     * PDF에서 텍스트를 추출하는 메서드
     */
    public String extractTextFromPdf(String filePath) {
        StringBuilder extractedText = new StringBuilder();

        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, DPI, ImageType.BINARY);
                extractedText.append(tesseract.doOCR(image)).append("\n");
            }
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("PDF 텍스트 추출 중 오류 발생: " + e.getMessage(), e);
        }

        return normalizeText(extractedText.toString());
    }
}
