package com.jobprep.resume_feedback.exception;

import com.jobprep.resume_feedback.common.exception.CommonError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResumeError implements CommonError {

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "40410", "파일을 찾을 수 없습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "40011", "지원하지 않는 파일 형식입니다."),
    FILE_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "50010", "파일 처리 중 오류가 발생했습니다."),
    TEXT_EXTRACTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "50011", "텍스트 추출 중 오류가 발생했습니다."),
    OCR_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "50012", "OCR 처리 중 오류가 발생했습니다."),
    AI_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "50313", "AI 서비스 오류가 발생했습니다."),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "40012", "빈 파일입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
