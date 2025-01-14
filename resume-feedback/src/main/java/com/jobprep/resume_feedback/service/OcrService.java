package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.domain.Resume;
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
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OcrService {
    public static final int DPI = 400;
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
            tesseract.setLanguage("kor+eng");  // 한국어와 영어 동시 인식
            tesseract.setPageSegMode(3);  // PSM 설정 (기본 페이지 세분화 모드)
        } catch (IOException e) {
            throw new RuntimeException("TESSDATA_PREFIX 설정 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * PDF 파일에서 이력서 정보를 추출하는 메서드
     */
    public Resume extractResumeFromPdf(String filePath) {
        String extractedText = performOcr(filePath);
        extractedText = normalizeText(extractedText);  // 정규화 추가
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
                BufferedImage processedImage = preprocessImage(image);
                extractedText.append(tesseract.doOCR(processedImage)).append("\n");
            }
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("OCR 처리 중 오류 발생: " + e.getMessage(), e);
        }

        return extractedText.toString();
    }

    /**
     * 이미지 전처리 - 흑백 변환 및 대비 조정
     */
    private BufferedImage preprocessImage(BufferedImage image) {
        RescaleOp rescaleOp = new RescaleOp(1.5f, 15, null);
        rescaleOp.filter(image, image);

        BufferedImage binaryImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY
        );
        binaryImage.getGraphics().drawImage(image, 0, 0, null);

        return binaryImage;
    }

    /**
     * OCR로 추출한 텍스트를 이력서 객체로 변환하는 메서드
     */
    private Resume parseExtractedTextToResume(String text) {
        String[] lines = text.split("\\n");

        // 기본 값 설정
        String selfIntroduction = "자기소개 없음";
        String technicalSkills = "기술스택 없음";
        String workExperience = "경력 없음";
        List<String> projects = new ArrayList<>();
        String activities = "대외활동 없음";

        // 줄별로 이력서 정보를 추출
        if (lines.length > 0) {
            selfIntroduction = lines[0].trim();
        }
        if (lines.length > 1) {
            technicalSkills = lines[1].trim();
        }
        if (lines.length > 2) {
            workExperience = lines[2].trim();
        }
        if (lines.length > 3) {
            // 네 번째 줄부터는 프로젝트 정보로 간주
            for (int i = 3; i < lines.length; i++) {
                projects.add(lines[i].trim());
            }
        }
        if (lines.length > 4) {
            workExperience = lines[4].trim();
        }

        return new Resume(selfIntroduction, technicalSkills, workExperience, projects, activities);
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
