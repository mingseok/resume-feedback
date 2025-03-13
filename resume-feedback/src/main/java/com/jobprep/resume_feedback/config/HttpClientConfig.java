package com.jobprep.resume_feedback.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    @Bean
    public CloseableHttpClient httpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(10);
        connectionManager.setValidateAfterInactivity(TimeValue.ofDays(5000));

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(5))   // 연결 타임아웃 (5초)
                        .setResponseTimeout(Timeout.ofSeconds(10)) // 응답 타임아웃 (10초)
                        .setConnectionRequestTimeout(Timeout.ofSeconds(3))
                        .build())
                .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE) // Keep-Alive 적용
                .evictExpiredConnections()
                .evictIdleConnections(Timeout.ofSeconds(30))
                .build();
    }
}
