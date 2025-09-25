package com.jobprep.resume_feedback.util;

import com.jobprep.resume_feedback.exception.FileUploadException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

public class FileHandler {

    private static final String FILE_PREFIX = "resume_";
    private static final String FILE_EXTENSION = ".pdf";

    public static String createTempFile(MultipartFile file) {
        try {
            File tempFile = File.createTempFile(FILE_PREFIX, FILE_EXTENSION);
            file.transferTo(tempFile);
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            throw new FileUploadException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    public static boolean isValidPdfFile(MultipartFile file) {
        return file != null && 
               !file.isEmpty() && 
               file.getOriginalFilename() != null &&
               file.getOriginalFilename().toLowerCase().endsWith(".pdf");
    }
}
