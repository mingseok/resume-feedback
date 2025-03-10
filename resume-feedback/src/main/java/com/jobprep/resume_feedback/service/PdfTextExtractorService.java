package com.jobprep.resume_feedback.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PdfTextExtractorService {

    private final Tesseract tesseract;

    @PostConstruct
    public void init() {
        try {
            File tessdataDir = new File(System.getProperty("java.io.tmpdir"), "tessdata");
            if (!tessdataDir.exists()) {
                tessdataDir.mkdirs();
            }

            tesseract.setDatapath(System.getProperty("java.io.tmpdir") + "/tessdata");
            tesseract.setPageSegMode(1);
            tesseract.setOcrEngineMode(1);
            tesseract.setVariable("user_defined_dpi", "300");
        } catch (Exception e) {
            throw new RuntimeException("Tesseract 초기화 오류: " + e.getMessage(), e);
        }
    }

    public String extractTextFromPdf(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            String text = new PDFTextStripper().getText(document).trim();
            if (!text.isEmpty()) {
                System.out.println("📌 텍스트 기반 PDF 감지됨 → OCR 대신 PDFTextStripper 사용");
                return text;
            }

            int dpi = determineDpi(new File(filePath).length());
            PDFRenderer renderer = new PDFRenderer(document);

            return IntStream.range(0, document.getNumberOfPages())
                    .mapToObj(page -> extractTextFromPage(renderer, page, dpi))
                    .collect(Collectors.joining("\n"))
                    .replaceAll("\\s+", " ")
                    .trim();
        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 로드 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private int determineDpi(long fileSize) {
        return 300;
    }

    private String extractTextFromPage(PDFRenderer renderer, int page, int dpi) {
        try {
            BufferedImage image = renderer.renderImageWithDPI(page, dpi, ImageType.RGB);
            if (image == null) {
                System.out.println("PDF 페이지 렌더링 실패: page=" + page);
                return "";
            }
            return extractTextFromImage(image);
        } catch (Exception e) {
            throw new RuntimeException("PDF 페이지 OCR 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private String extractTextFromImage(BufferedImage image) {
        try {
            BufferedImage preprocessedImage = preprocessImage(image);
            return tesseract.doOCR(preprocessedImage).replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            throw new RuntimeException("OCR 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private BufferedImage preprocessImage(BufferedImage image) {
        BufferedImage grayImage = toGrayscale(image);
        BufferedImage contrastEnhanced = adjustContrast(grayImage, 1.8f, 20); // ✅ 대비 조정 값 변경
        return applyGaussianBlur(contrastEnhanced, 2); // ✅ 블러 강도 조정
    }

    private BufferedImage toGrayscale(BufferedImage image) {
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return grayImage;
    }

    private BufferedImage adjustContrast(BufferedImage image, float scaleFactor, float offset) {
        RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        rescaleOp.filter(image, result);
        return result;
    }

    private BufferedImage applyGaussianBlur(BufferedImage image, int radius) {
        int size = radius * 2 + 1;
        float[] matrix = new float[size * size];
        float sigma = radius / 3.0f;
        float sum = 0;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float value = (float) Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                matrix[(y + radius) * size + (x + radius)] = value;
                sum += value;
            }
        }

        for (int i = 0; i < matrix.length; i++) {
            matrix[i] /= sum;
        }

        Kernel kernel = new Kernel(size, size, matrix);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(image, null);
    }
}
