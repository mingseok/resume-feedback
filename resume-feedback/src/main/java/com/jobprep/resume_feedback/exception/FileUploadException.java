package com.jobprep.resume_feedback.exception;

public class FileUploadException extends RuntimeException {
    public FileUploadException(String message) {
        super("파일 업로드 오류: " + message);
    }
}
