package com.jobprep.resume_feedback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobprep.resume_feedback.exception.ResumeError;
import com.jobprep.resume_feedback.exception.ResumeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.url}")
    private String apiUrl;

    public String requestCompletion(Map<String, Object> requestBody) {
        for (int retryCount = 0; retryCount < MAX_RETRIES; retryCount++) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost post = createHttpPost(requestBody);
                try (CloseableHttpResponse response = httpClient.execute(post)) {
                    return new String(response.getEntity().getContent().readAllBytes());
                }
            } catch (IOException e) {
                log.error("HTTP 요청 실패, 재시도 횟수: {}", retryCount + 1, e);
                if (retryCount < MAX_RETRIES - 1) {
                    waitForRetry();
                }
            }
        }
        throw new ResumeException(ResumeError.AI_SERVICE_ERROR);
    }

    private HttpPost createHttpPost(Map<String, Object> requestBody) {
        try {
            HttpPost post = new HttpPost(apiUrl);
            post.setHeader("Authorization", "Bearer " + apiKey);
            post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
            post.setEntity(new StringEntity(new ObjectMapper().writeValueAsString(requestBody), ContentType.APPLICATION_JSON));
            return post;
        } catch (Exception e) {
            throw new ResumeException(ResumeError.AI_SERVICE_ERROR);
        }
    }

    private void waitForRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("재시도 대기 중 인터럽트 발생", e);
        }
    }
}
