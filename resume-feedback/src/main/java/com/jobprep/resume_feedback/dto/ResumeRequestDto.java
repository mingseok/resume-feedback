package com.jobprep.resume_feedback.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Data
public class ResumeRequestDto {
    private MultipartFile file;

    // 파일 경로 반환
    public String getFilePath() {
        try {
            // 임시 디렉토리에 파일 저장
            File tempFile = File.createTempFile("resume_", ".pdf");
            file.transferTo(tempFile);
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("파일 경로를 가져올 수 없습니다.", e);
        }
    }
}
