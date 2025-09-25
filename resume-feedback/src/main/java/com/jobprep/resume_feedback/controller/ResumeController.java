package com.jobprep.resume_feedback.controller;

import com.jobprep.resume_feedback.common.dto.ApiResponse;
import com.jobprep.resume_feedback.service.ResumeProcessingService;
import com.jobprep.resume_feedback.service.dto.request.ResumeRequest;
import com.jobprep.resume_feedback.service.dto.response.FeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Controller
@Validated
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeProcessingService resumeProcessingService;

    @GetMapping("/")
    public String showUploadPage() {
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        ResumeRequest request = ResumeRequest.from(file);
        FeedbackResponse feedback = resumeProcessingService.processResume(request);
        model.addAttribute("result", feedback);
        return "result";
    }

    @PostMapping(value = "/api/resume/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<ApiResponse<FeedbackResponse>> analyzeResume(
            @RequestParam("file") MultipartFile file) {
        
        ResumeRequest request = ResumeRequest.from(file);
        FeedbackResponse feedback = resumeProcessingService.processResume(request);
        
        return ResponseEntity.ok(ApiResponse.success(feedback));
    }

    @GetMapping("/result")
    public String showResultPage() {
        return "result";
    }
}
