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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Validated
@RequiredArgsConstructor
public class FileUploadController {

    private final ResumeService resumeService;

    @GetMapping("/")
    public String showUploadPage() {
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            ResumeRequestDto requestDto = new ResumeRequestDto(file);
            FeedbackResponseDto feedback = resumeService.processResume(requestDto);
            model.addAttribute("result", feedback);
            return "result";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    // 결과 페이지
    @GetMapping("/result")
    public String showResultPage() {
        return "result";
    }
}
