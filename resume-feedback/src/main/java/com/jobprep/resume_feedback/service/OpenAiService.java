package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    public String getDetailedFeedback(String content) {
        // TODO: 공고도 같이 올리면 어떨지에 대한 질문 작성.

        String[] questions = {
                "기본 정보는 적절한가? 이력서에 이름, 연락처, 이메일 주소 등이 포함되어 있는지 확인하고, 적절한 형식으로 작성되었는지 평가해주세요.",
                "기술 스택은 잘 구성되어 있는가? 이력서에 나열된 기술 스택이 직무에 적합한지, 그리고 충분히 설명되었는지 평가해주세요.",
                "경력 사항은 충분한가? 직무와 관련된 경력 사항이 충분히 기술되어 있는지, 주요 성과가 포함되어 있는지 평가해주세요.",
                "포트폴리오는 잘 작성되어 있는가? 포트폴리오가 포함되어 있다면, 직무와 관련된 프로젝트를 충분히 보여주고 있는지 평가해주세요.",
                "대외활동은 관련성이 있는가? 이력서에 기재된 대외활동이 지원하는 직무와 관련이 있는지, 어떤 경험이 도움이 되는지 평가해주세요.",
                "자격증 여부는 어떠한가? 직무에 필요한 자격증이 있는지, 자격증이 직무와 관련된 내용인지 평가해주세요.",
                "자기소개서는 자연스러운가? 이력서에 포함된 자기소개서가 지원하는 직무에 적합하게 작성되었는지, 그리고 본인의 강점을 잘 보여주는지 평가해주세요."
        };

        String questionsPrompt = Arrays.stream(questions)
                .collect(Collectors.joining("\n\n"));

        String prompt = "다음 이력서를 평가하고 각 항목에 대한 구체적인 피드백을 제공해줘:\n\n"
                + questionsPrompt
                + "\n\n이력서 내용:\n" + content;

        return callOpenAiApi(prompt);
    }

    // OpenAI API 호출 메서드
    public String callOpenAiApi(String text) {
        try {
            // 요청 본문 생성 (이전 예시에서 JSON 본문 생성 메서드 사용)
            String requestBody = createRequestBody(text);

            // HTTP 요청 생성
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // HTTP 클라이언트 생성 및 요청 보내기
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 응답 본문 출력
            System.out.println("Raw API response: " + response.body());

            // JSON 응답 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResponse = objectMapper.readTree(response.body());

            // 응답에서 message.content 추출
            String content = jsonResponse.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // 추출된 내용 출력
            System.out.println("GPT 응답: " + content);

            // 추출된 응답 반환
            return content;

        } catch (Exception e) {
            e.printStackTrace();
            return "OpenAI API 호출 중 오류 발생";
        }
    }

    // OpenAI API 요청 본문 생성 메서드
    private String createRequestBody(String text) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> body = new HashMap<>();
            body.put("model", "gpt-3.5-turbo"); // 모델을 gpt-3.5-turbo로 설정

            // messages 배열 생성
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user"); // 역할은 user로 설정
            userMessage.put("content", text); // 텍스트를 content로 설정
            messages.add(userMessage);

            body.put("messages", messages); // messages 배열을 요청 본문에 추가
            body.put("max_tokens", 1600); // 최대 토큰 설정

            // 객체를 JSON 문자열로 변환
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, String> parseFeedbackByCategory(String feedback) {
        Map<String, String> categoryFeedback = new HashMap<>();
        String[] categories = {
                "기본 정보", "기술 스택", "경력 사항",
                "포트폴리오", "대외활동", "자격증", "자기소개서"
        };

        for (String category : categories) {
            String regex = category + "([^\\n]+)([\\s\\S]*?)(?=\\n[A-Z가-힣\\s]+:|$)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(feedback);
            if (matcher.find()) {
                categoryFeedback.put(category, matcher.group(2).trim());
            } else {
                // 카테고리가 누락된 경우 기본 메시지를 추가
                categoryFeedback.put(category, "해당 항목에 대한 피드백이 없습니다.");
            }
        }
        return categoryFeedback;
    }

    public String requestFurtherEvaluation(String category, String feedback) {
        String prompt = "다음은 '" + category + "'에 대한 피드백입니다:\n\n" + feedback
                + "\n\n이 내용을 바탕으로 추가 평가를 제공해주세요.";
        return callOpenAiApi(prompt);
    }
}
