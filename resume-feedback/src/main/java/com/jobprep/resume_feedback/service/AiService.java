package com.jobprep.resume_feedback.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;

@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build(); // ## ChatClient 인스턴스 생성
    }

    // ## 비동기 처리와 논블로킹 적용
    public Mono<String> getFeedbackForQuestionAsync(String question, String content) {
        long startTime = System.currentTimeMillis();
        System.out.println("비동기 시작: " + question + " at " + startTime + " | Thread: " + Thread.currentThread().getName());

        // 블로킹 호출을 별도의 스레드풀에서 실행
        return Mono.fromCallable(() -> {
                    var response = chatClient.prompt()
                            .user(question + "\n\n이력서 내용:\n" + content)
                            .call();
                    return response.content();
                })
                .subscribeOn(Schedulers.boundedElastic()) // 별도 스레드풀 사용
                .doOnNext(result -> {
                    long endTime = System.currentTimeMillis();
                    System.out.println("비동기 종료: " + question + " at " + endTime + " (처리 시간: " + (endTime - startTime) + "ms) | Thread: " + Thread.currentThread().getName());
                });
    }

    public Mono<Map<String, String>> getFeedbackForSectionsAsync(String content) {
        String preprocessedContent = preprocessResumeText(content);

        String[] questions = {
                "이력서의 기본 정보(이름, 연락처, 이메일 등)가 적절하게 포함되고 형식이 올바른지 평가해주세요.",
                "기술 스택이 직무에 적합하고 충분히 설명되었는지 평가해주세요.",
                "경력 사항과 포트폴리오가 직무와 연관성이 높고 주요 성과가 잘 드러나 있는지 평가해주세요.",
                "대외활동과 자격증이 직무와 관련성이 있으며, 지원자의 역량을 보완하는지 평가해주세요.",
                "자기소개서가 직무와 적합하고, 지원자의 강점과 가치관을 잘 전달하고 있는지 평가해주세요."
        };

        // 질문과 응답을 병렬로 처리하며 Map으로 수집
        return Flux.fromArray(questions)
                .flatMap(question ->
                                getFeedbackForQuestionAsync(question, preprocessedContent)
                                        .map(response -> Tuples.of(question, response)), // Tuple2<String, String> 생성
                        2) // 병렬 요청 수 제한
                .collectMap(Tuple2::getT1, Tuple2::getT2); // Tuple2에서 Key와 Value 추출
    }

    // $$ 처리 시간을 비교하여 단축 비율을 계산하는 메서드 추가
    public void calculateTimeReduction(long syncTime, long asyncTime) {
        long reduction = syncTime - asyncTime;
        double reductionPercentage = ((double) reduction / syncTime) * 100;

        System.out.printf("비동기 처리 시간: %dms, 동기 처리 시간: %dms, 단축 시간: %dms, 단축 비율: %.2f%%%n",
                asyncTime, syncTime, reduction, reductionPercentage);
    }

    private String preprocessResumeText(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "입력된 텍스트가 없습니다. 유효한 텍스트를 입력해주세요.";
        }
        String cleanedText = rawText.replaceAll("[^가-힣a-zA-Z0-9.,!?\\s]", " ");
        return cleanedText.replaceAll("\\s+", " ").trim();
    }
}