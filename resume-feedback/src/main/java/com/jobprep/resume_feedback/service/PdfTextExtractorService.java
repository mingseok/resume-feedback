package com.jobprep.resume_feedback.service;

import com.sun.management.OperatingSystemMXBean;
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
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PdfTextExtractorService {

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
     * PDF 파일에서 텍스트를 추출합니다.
     *
     * @param filePath PDF 파일 경로
     * @return 추출된 텍스트
     */
    public String extractTextFromPdf(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            // 파일 크기에 따라 적절한 DPI 값 계산
            long fileSize = new File(filePath).length();
            int dpi = determineDpi(fileSize);

            PDFRenderer renderer = new PDFRenderer(document);

            // 각 페이지의 텍스트를 추출하여 합치기
            return IntStream.range(0, document.getNumberOfPages())
                    .mapToObj(page -> extractTextFromPage(renderer, page, dpi))
                    .reduce("", (text1, text2) -> text1 + "\n" + text2);
        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 로드 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 크기 및 CPU 사용량을 기반으로 동적으로 DPI를 결정합니다.
     *
     * @param fileSize 파일 크기 (바이트 단위)
     * @return 결정된 DPI 값
     */
    private int determineDpi(long fileSize) {
        if (isCpuUsageHigh()) { // CPU 사용량이 높은 경우 최소 DPI로 설정
            return 200;
        }

        if (fileSize <= 500 * 1024) { // 500KB 이하
            return 200;
        } else if (fileSize <= 1024 * 1024) { // 500KB ~ 1MB
            return 250;
        } else { // 1MB 이상
            return 300;
        }
    }

    /**
     * PDF의 특정 페이지에서 OCR을 통해 텍스트를 추출합니다.
     *
     * @param renderer PDFRenderer 객체
     * @param page 페이지 번호 (0부터 시작)
     * @param dpi 페이지를 이미지로 변환할 때 사용할 DPI 값
     * @return OCR로 추출한 정규화된 텍스트
     */
    private String extractTextFromPage(PDFRenderer renderer, int page, int dpi) {
        try {
            // 페이지를 이미지로 렌더링
            BufferedImage image = renderer.renderImageWithDPI(page, dpi, ImageType.BINARY);
            // OCR로 텍스트 추출 및 정규화
            return normalizeText(tesseract.doOCR(image));
        } catch (Exception e) {
            throw new RuntimeException("페이지 " + page + " 텍스트 추출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 현재 시스템의 CPU 사용량이 높은지 확인합니다.
     *
     * @return CPU 사용량이 80%를 초과하면 true, 그렇지 않으면 false
     */
    private boolean isCpuUsageHigh() {
        try {
            // 시스템 CPU 정보를 가져옴
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getCpuLoad(); // CPU 사용률 (0.0 ~ 1.0)
            return cpuLoad > 0.8; // CPU 사용량이 80% 초과 여부 확인
        } catch (Exception e) {
            System.err.println("CPU 사용량 확인 중 오류 발생: " + e.getMessage());
            return false; // 예외 발생 시 기본적으로 false 반환
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
