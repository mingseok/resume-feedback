package com.jobprep.resume_feedback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PromptBuilder {

    @Value("${spring.ai.openai.model}")
    private String model;

    public Map<String, Object> buildRequestBody(Resume resume) {
        String prompt = createPrompt(resume);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a professional resume reviewer."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 2000);
        return requestBody;
    }

    private String createPrompt(Resume resume) {
        return """
                당신은 전문적인 이력서 리뷰어입니다.
                아래 이력서를 분석하고, **반드시** JSON 형식으로만 피드백을 제공합니다.
                Chain-of-Thought(CoT) + Few-shot Learning 방식으로 상세한 피드백을 JSON 형식으로 제공합니다.
                
                ### 요청 형식:
                - 각 항목별로 **상세한 피드백**을 JSON 객체 형태로 작성합니다.
                - **JSON 외 다른 텍스트를 포함하지 마세요.**
                - **JSON 형식이 올바르지 않으면 요청이 실패합니다.**
                
                ### JSON 응답 예시 (이 형식을 따라야 합니다!):
                {
                "자기소개": "지원하는 직무와 연관성을 강조하고, 구체적인 프로젝트 경험을 추가하면 좋습니다. 예를 들어, 백엔드 개발자로 지원하는 경우 'Spring Boot 기반의 REST API 개발 경험'을 명확하게 기재하는 것이 유리합니다.", \s
                "기술 스택": "기본적인 기술 외에도 SQL과 Redis 활용 경험을 강조하면 좋습니다. 예를 들어, 'Redis를 활용한 캐싱으로 API 응답 속도를 40% 향상시킨 경험'을 기술하면 더 효과적입니다.", \s
                "경력": "각 업무별 성과를 수치로 표현하면 더 효과적입니다. 예를 들어, 'AWS 비용 최적화를 통해 인프라 비용 30% 절감'과 같은 구체적인 수치를 추가하세요.", \s
                "프로젝트": "성공한 사례를 중심으로 기술적 기여도를 강조하면 좋습니다. 예를 들어, '비동기 요청을 적용하여 AI 서빙 속도를 40% 단축'한 경험을 기재하면 강점이 부각됩니다.", \s
                "대외활동": "해당 활동이 직무에 어떤 영향을 주었는지 설명하면 좋습니다. 예를 들어, '오픈소스 프로젝트 기여 경험을 통해 코드 리뷰 및 협업 역량을 강화'한 사례를 언급하세요." \s
                }
                
                **중요!**
                - JSON 코드 블록 없이 **순수 JSON 데이터만 반환**하세요.
                - JSON이 아닌 응답이 나오면 요청이 실패합니다.
                
                이제 아래 이력서를 분석하고, 위 JSON 형식과 정확히 일치하는 JSON 응답을 반환하세요.
                
                이력서 내용:
                """ + resume.toString();
    }
}
