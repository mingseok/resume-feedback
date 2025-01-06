package com.jobprep.resume_feedback.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TesseractConfig {

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("src/main/resources/tessdata/");
        tesseract.setLanguage("kor");
        return tesseract;
    }
}
