package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    public OpenAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, String> getFeedbackForSections(String content, StringBuilder logSummary) {
        // 깨진 텍스트 필터링
        String preprocessedContent = cleanText(content);

        // 텍스트가 유효한지 확인
        if (preprocessedContent.equals("입력된 텍스트가 너무 짧아 분석이 어렵습니다. 텍스트를 보완해주세요.")) {
            return Map.of("error", preprocessedContent);
        }

        // 질문과 대응되는 간략한 제목 매핑
        String[] shortTitles = {
                "기본 정보 피드백 응답 시간",
                "기술 스택 피드백 응답 시간",
                "경력 사항 피드백 응답 시간",
                "대외활동, 자격증 피드백 응답 시간",
                "자기소개서 피드백 응답 시간"
        };

        String[] questions = {
                "이력서의 기본 정보(이름, 연락처, 이메일 등)가 적절하게 포함되고 형식이 올바른지 평가해주세요.",
                "기술 스택이 직무에 적합하고 충분히 설명되었는지 평가해주세요. 주요 기술에 대한 이해를 보여주는지 확인해주세요.",
                "경력 사항과 포트폴리오가 직무와 연관성이 높고 주요 성과가 잘 드러나 있는지 평가해주세요.",
                "대외활동과 자격증이 직무와 관련성이 있으며, 지원자의 역량을 보완하는지 평가해주세요.",
                "자기소개서가 직무와 적합하고, 지원자의 강점과 가치관을 잘 전달하고 있는지 평가해주세요."
        };

        Map<String, String> feedbackMap = new HashMap<>();
        long totalApiTime = 0;

        // ** 텍스트 전처리 적용 **
        String processedContent = preprocessResumeText(content);

        for (int i = 0; i < questions.length; i++) {
            long startTime = System.currentTimeMillis();
            String response = callOpenAiApi(questions[i], processedContent);
            long endTime = System.currentTimeMillis();

            // 각 질문에 대해 간단한 제목으로 로그에 추가
            logSummary.append(String.format("%s: %d ms\n", shortTitles[i], endTime - startTime));
            feedbackMap.put(questions[i], response);
            totalApiTime += (endTime - startTime);
        }

        return feedbackMap;
    }

    private String callOpenAiApi(String question, String content) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", new Object[]{
                Map.of("role", "system", "content", "You are a helpful assistant."),
                Map.of("role", "user", "content", question + "\n\n이력서 내용:\n" + content)
        });

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 응답 데이터를 byte[]로 수신
            ResponseEntity<byte[]> response = restTemplate.postForEntity(apiUrl, entity, byte[].class);

            // 응답 상태 코드 확인
            System.out.println("OpenAI API 응답 상태 코드: " + response.getStatusCode());

            // 응답 본문 처리 (압축 해제 또는 문자열 변환)
            String responseBody = new String(response.getBody(), StandardCharsets.UTF_8);

            // 응답 로그
            System.out.println("OpenAI API 응답 본문: " + responseBody);

            // JSON 데이터에서 메시지 추출
            return extractMessageFromJson(responseBody);
        } catch (Exception e) {
            // 에러 로그
            System.err.println("OpenAI API 호출 중 오류: " + e.getMessage());
            e.printStackTrace();
            return "API 호출 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String extractMessageFromJson(String responseBody) {
        try {
            // JSON 파싱을 위해 ObjectMapper 사용
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);

            if (responseMap.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                for (Map<String, Object> choice : choices) {
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    if (message != null && message.containsKey("content")) {
                        return message.get("content");
                    }
                }
            }
            return "API 응답에서 메시지를 찾을 수 없습니다.";
        } catch (Exception e) {
            System.err.println("응답 JSON 파싱 중 오류: " + e.getMessage());
            e.printStackTrace();
            return "응답 파싱 중 오류가 발생했습니다.";
        }
    }

    private String extractMessageFromResponse(Map<String, Object> response) {
        if (response == null || !response.containsKey("choices")) {
            return "API 응답이 비어있습니다.";
        }

        var choices = (Iterable<Map<String, Object>>) response.get("choices");
        for (Map<String, Object> choice : choices) {
            Map<String, String> message = (Map<String, String>) choice.get("message");
            if (message != null && message.containsKey("content")) {
                return message.get("content");
            }
        }
        return "API 응답에서 메시지를 찾을 수 없습니다.";
    }

    private String preprocessResumeText(String rawText) {
        // 불필요한 공백 제거
        rawText = rawText.replaceAll("\\s+", " ").trim();

        // 최대 길이 제한
        int maxLength = 3000; // OpenAI API가 처리하기 적합한 크기
        return rawText.length() > maxLength ? rawText.substring(0, maxLength) : rawText;
    }

    private String cleanText(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }

        // 정규식으로 유효한 문자만 남기기 (한글, 영어, 숫자, 기본 구두점)
        String cleanedText = rawText.replaceAll("[^가-힣a-zA-Z0-9.,!?\\s]", " ");

        // 다중 공백 제거
        cleanedText = cleanedText.replaceAll("\\s+", " ").trim();

        // 텍스트가 너무 짧으면 기본 메시지 반환
        if (cleanedText.length() < 50) {
            return "입력된 텍스트가 너무 짧아 분석이 어렵습니다. 텍스트를 보완해주세요.";
        }

        return cleanedText;
    }
}
