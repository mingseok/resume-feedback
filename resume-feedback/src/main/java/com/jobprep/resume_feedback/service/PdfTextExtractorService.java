package com.jobprep.resume_feedback.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PdfTextExtractorService {

    public static final int DPI = 370; // PDF 렌더링 해상도 설정

    private final ResourceLoader resourceLoader;
    private final Tesseract tesseract;

    /**
     * Tesseract 초기화 (데이터 경로 및 언어 설정)
     */
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
     * PDF 문서의 모든 페이지에서 텍스트를 추출합니다.
     * @param filePath PDF 파일 경로
     * @return 추출된 정규화 텍스트
     */
    public String extractTextFromPdf(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            return IntStream.range(0, document.getNumberOfPages())
                    .mapToObj(page -> extractTextFromPage(renderer, page))
                    .reduce("", (text1, text2) -> text1 + "\n" + text2);
        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 로드 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * PDF 문서의 특정 페이지에서 텍스트를 추출합니다.
     * @param renderer PDFRenderer 객체
     * @param page 페이지 번호
     * @return OCR로 추출된 정규화 텍스트
     */
    private String extractTextFromPage(PDFRenderer renderer, int page) {
        try {
            BufferedImage image = renderer.renderImageWithDPI(page, DPI, ImageType.BINARY);
            return normalizeText(tesseract.doOCR(image));
        } catch (Exception e) {
            throw new RuntimeException("페이지 " + page + " 텍스트 추출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 텍스트를 정규화하여 불필요한 공백과 특수문자를 제거합니다.
     * @param text 원본 텍스트
     * @return 정규화된 텍스트
     */
    private String normalizeText(String text) {
        return text.replaceAll("[^가-힣a-zA-Z0-9\\s]", "") // 한글, 영어, 숫자, 공백만 유지
                .replaceAll("\\s{2,}", " ")               // 중복 공백 제거
                .trim();
    }
}
