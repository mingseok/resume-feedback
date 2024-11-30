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

@Service
@RequiredArgsConstructor
public class OcrService {

    private final Tesseract tesseract = new Tesseract();

    @PostConstruct
    public void init() {
        try {
            // ClassPathResource로 tessdata 경로 가져오기
            String absolutePath = new ClassPathResource("tessdata").getFile().getAbsolutePath();
            System.out.println("TESSDATA_PREFIX 절대 경로: " + absolutePath);

            // tessdata 디렉토리 존재 여부 확인
            File tessdataDir = new File(absolutePath);
            if (!tessdataDir.exists() || !tessdataDir.isDirectory()) {
                throw new RuntimeException("tessdata 디렉토리가 존재하지 않습니다: " + tessdataDir.getAbsolutePath());
            }

            // kor.traineddata 파일 확인
            File korFile = new File(tessdataDir, "kor.traineddata");
            if (!korFile.exists()) {
                throw new RuntimeException("훈련 데이터 파일이 없습니다: " + korFile.getAbsolutePath());
            }

            // Tesseract 데이터 경로 및 언어 설정
            tesseract.setDatapath(absolutePath);
            tesseract.setLanguage("kor");
            System.out.println("훈련 데이터 파일 로드 성공: " + korFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("tessdata 경로를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    public String extractTextFromPdfWithOcr(File pdfFile) {
        StringBuilder extractedText = new StringBuilder();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.BINARY);
                String pageText = tesseract.doOCR(image);
                extractedText.append(pageText);
            }

            String result = extractedText.toString();
            System.out.println("result = " + result);
            return result;
        } catch (TesseractException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
