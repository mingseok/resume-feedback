package com.jobprep.resume_feedback.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.IOException;

@Getter
@Setter
@NoArgsConstructor
public class ResumeRequestDto {

    @NotNull(message = "이력서 파일은 필수입니다.")
    private MultipartFile file;

    public ResumeRequestDto(MultipartFile file) {
        this.file = file;
    }

    // 파일 내용을 문자열로 반환하는 메서드
    public String getFileContent() {
        try {
            return new String(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("파일 내용을 읽는 중 오류 발생", e);
        }
    }
}