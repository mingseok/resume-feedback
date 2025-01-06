package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.domain.Resume;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.ResourceLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OcrService {
    public static final int DPI = 150;
    public static final String DEFAULT_NAME = "Unknown";
    public static final String DEFAULT_CONTACT = "Unknown";
    public static final String DEFAULT_TECHNICAL_SKILLS = "Unknown";

    private final ResourceLoader resourceLoader;
    private final Tesseract tesseract;

    @PostConstruct
    public void init() {
        try {
            String tessdataPath = Paths.get(resourceLoader.getResource("classpath:tessdata").getURI()).toString();
            tesseract.setDatapath(tessdataPath);
        } catch (IOException e) {
            throw new RuntimeException("TESSDATA_PREFIX 설정 중 오류 발생: " + e.getMessage(), e);
        }
        tesseract.setLanguage("kor");
    }

    /**
     * PDF 파일에서 이력서 정보를 추출하는 메서드
     */
    public Resume extractResumeFromPdf(String filePath) {
        String extractedText = performOcr(filePath);
        return parseExtractedTextToResume(extractedText);
    }

    /**
     * PDF 파일을 OCR 처리하여 텍스트를 추출하는 메서드
     */
    private String performOcr(String filePath) {
        File pdfFile = new File(filePath);
        StringBuilder extractedText = new StringBuilder();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, DPI, ImageType.RGB);
                extractedText.append(tesseract.doOCR(image)).append("\n");
            }
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("OCR 처리 중 오류 발생: " + e.getMessage(), e);
        }

        return extractedText.toString();
    }

    /**
     * OCR로 추출한 텍스트를 이력서 객체로 변환하는 메서드
     */
    private Resume parseExtractedTextToResume(String text) {
        String[] lines = text.split("\\n");

        String name = DEFAULT_NAME;
        String contactInfo = DEFAULT_CONTACT;
        String technicalSkills = DEFAULT_TECHNICAL_SKILLS;
        List<String> projects = new ArrayList<>();

        if (lines.length > 0) {
            name = lines[0].trim();
        }

        if (lines.length > 1) {
            contactInfo = lines[1].trim();
        }

        if (lines.length > 2) {
            technicalSkills = lines[2].trim();
        }

        if (lines.length > 3) {
            for (int i = 3; i < lines.length; i++) {
                projects.add(lines[i].trim());
            }
        }

        return new Resume(name, contactInfo, technicalSkills, projects);
    }
}
