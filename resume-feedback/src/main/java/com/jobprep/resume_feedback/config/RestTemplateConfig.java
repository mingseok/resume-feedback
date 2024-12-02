package com.jobprep.resume_feedback.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // HTTP 클라이언트 구성
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofDays(5000)) // 연결 타임아웃 (ms)
                        .setConnectTimeout(Timeout.ofDays(10000)) // 읽기 타임아웃 (ms)
                        .build())
                .build();

        // HttpComponentsClientHttpRequestFactory 사용
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // RestTemplate 생성
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        // 메시지 컨버터 추가
        restTemplate.getMessageConverters().add(0, new ByteArrayHttpMessageConverter()); // 바이너리 데이터 처리
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter()); // JSON 처리

        return restTemplate;
    }
}
