package com.jobprep.resume_feedback.controller;

import com.jobprep.resume_feedback.dto.FeedbackResponseDto;
import com.jobprep.resume_feedback.dto.ResumeRequestDto;
import com.jobprep.resume_feedback.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@Validated
@RequiredArgsConstructor
public class FileUploadController {

    private final ResumeService resumeService;

    @GetMapping("/")
    public String showUploadPage() {
        return "upload";
    }

    // 파일 업로드 처리 및 로딩 페이지로 이동
    @PostMapping("/upload")
    public String handleFileUpload(ResumeRequestDto requestDto) {
        resumeService.processResumeAsync(requestDto);
        return "redirect:/loading";
    }

    // 진행률 업데이트 (SSE 연결)
    @GetMapping("/progress")
    public SseEmitter getProgress() {
        SseEmitter emitter = new SseEmitter(60000L); // 타임아웃 설정
        resumeService.subscribeToProgress(emitter);
        return emitter;
    }

    // 로딩 페이지
    @GetMapping("/loading")
    public String showLoadingPage() {
        return "loading";  // 로딩 페이지로 이동
    }

    // 결과 페이지
    @GetMapping("/result")
    public String showResultPage(Model model) {
        FeedbackResponseDto feedback = resumeService.getFeedback();
        model.addAttribute("result", feedback);
        return "result";
    }
}
