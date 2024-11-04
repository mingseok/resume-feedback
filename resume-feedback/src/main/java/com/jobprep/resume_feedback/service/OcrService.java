package com.jobprep.resume_feedback.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class OcrService {

    @Value("${tessdata.prefix}")
    private String tessDataPath;

    private final Tesseract tesseract = new Tesseract();

    @PostConstruct
    public void init() {
        // Tesseract 설정에 yml에서 주입받은 tessDataPath를 사용
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("kor");  // 필요한 언어 설정
    }

    public String extractTextFromPdfWithOcr(File pdfFile) throws IOException, TesseractException {
        StringBuilder extractedText = new StringBuilder();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.GRAY);
                String pageText = tesseract.doOCR(image);
                extractedText.append(pageText);
            }
        }
        return extractedText.toString();
    }
}
