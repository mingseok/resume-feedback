package com.jobprep.resume_feedback.common.exception;

import com.jobprep.resume_feedback.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException ex) {
        CommonError commonError = ex.getCommonError();
        log.warn("ServiceException 발생: {}", commonError.getMessage());
        return new ResponseEntity<>(
                ApiResponse.failure(commonError.getMessage()),
                commonError.getHttpStatus()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMessage = ErrorCode.BAD_REQUEST.getMessage();
        if (fieldError != null) {
            errorMessage = fieldError.getDefaultMessage();
        }

        log.warn("MethodArgumentNotValidException 발생: {}", errorMessage);
        return new ResponseEntity<>(
                ApiResponse.failure(errorMessage),
                ErrorCode.BAD_REQUEST.getHttpStatus()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("파일 업로드 크기 초과: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.failure("업로드 파일 크기가 제한을 초과했습니다."),
                HttpStatus.PAYLOAD_TOO_LARGE
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("처리되지 않은 예외 발생", ex);
        return new ResponseEntity<>(
                ApiResponse.failure("서버 오류가 발생했습니다."),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("요청한 정적 리소스를 찾을 수 없습니다: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }
}
