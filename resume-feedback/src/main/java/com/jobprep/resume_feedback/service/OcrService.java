package com.jobprep.resume_feedback.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
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

    private String tessDataPath; // 'final' 키워드 제거

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource tessResource = new ClassPathResource("tessdata/kor.traineddata");

        File tessDir = Files.createTempDirectory("tessdata").toFile();  // 임시 디렉토리 생성
        tessDataPath = tessDir.getAbsolutePath();

        // 임시 디렉토리에 리소스 복사
        try (InputStream is = tessResource.getInputStream()) {
            File targetFile = new File(tessDir, "kor.traineddata");
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tessDir.deleteOnExit();
        }

        // Tesseract 설정에 임시 파일 경로 사용
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("kor");
    }

    public String extractTextFromPdfWithOcr(File pdfFile) throws IOException, TesseractException {
        StringBuilder extractedText = new StringBuilder();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("kor");

            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.GRAY);
                String pageText = tesseract.doOCR(image);
                extractedText.append(pageText);
            }
        }
        return extractedText.toString();
    }
}
