package com.jobprep.resume_feedback.service.dto.request;

import com.jobprep.resume_feedback.exception.ResumeError;
import com.jobprep.resume_feedback.exception.ResumeException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ResumeRequest {
    
    private final MultipartFile file;

    public static ResumeRequest from(MultipartFile file) {
        validateFile(file);
        return new ResumeRequest(file);
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeException(ResumeError.EMPTY_FILE);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new ResumeException(ResumeError.INVALID_FILE_TYPE);
        }
    }

    public String getOriginalFilename() {
        return file.getOriginalFilename();
    }

    public long getSize() {
        return file.getSize();
    }

    public String getContentType() {
        return file.getContentType();
    }
}
