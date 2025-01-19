package com.jobprep.resume_feedback.controller;

import com.jobprep.resume_feedback.dto.FeedbackResponseDto;
import com.jobprep.resume_feedback.dto.ResumeRequestDto;
import com.jobprep.resume_feedback.service.ResumeProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Validated
@RequiredArgsConstructor
public class FileUploadController {

    private final ResumeProcessingService resumeProcessingService;

    /**
     * 업로드 페이지를 보여줍니다.
     */
    @GetMapping("/")
    public String showUploadPage() {
        return "upload";
    }

    /**
     * 파일 업로드를 처리하고 결과를 보여줍니다.
     *
     * @param file  업로드된 파일
     * @param model 모델 객체
     */
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        try {
            ResumeRequestDto resumeRequest = new ResumeRequestDto(file);
            FeedbackResponseDto feedback = resumeProcessingService.processResume(resumeRequest);
            model.addAttribute("result", feedback);
            return "result";
        } catch (Exception e) {
            // 로그에 예외 메시지 기록
            System.err.println("파일 처리 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "파일 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
            return "upload";
        }
    }

    /**
     * 결과 페이지를 보여줍니다.
     */
    @GetMapping("/result")
    public String showResultPage() {
        return "result";
    }
}
