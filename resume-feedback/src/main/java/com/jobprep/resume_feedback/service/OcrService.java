package com.jobprep.resume_feedback.service;

import com.jobprep.resume_feedback.util.ByteArrayPool;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class OcrService {

    @Value("${tessdata.prefix}")
    private String tessDataPath;

    private final Tesseract tesseract = new Tesseract();
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final ByteArrayPool byteArrayPool = new ByteArrayPool(1024 * 1024, 10);

    @PostConstruct
    public void init() {
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("kor");  // 필요한 언어 설정
    }

    public String extractTextFromPdfWithOcr(File pdfFile) {
        StringBuilder extractedText = new StringBuilder();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage image = renderer.renderImageWithDPI(page, 150, ImageType.GRAY);

                synchronized (byteArrayOutputStream) {
                    byteArrayOutputStream.reset();  // 이전 데이터를 지우고 재사용
                    try {
                        String pageText = tesseract.doOCR(image);
                        byteArrayOutputStream.write(pageText.getBytes(StandardCharsets.UTF_8));
                    } catch (TesseractException e) {
                        e.printStackTrace();
                    }
                    extractedText.append(byteArrayOutputStream.toString(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return extractedText.toString();
    }
}
