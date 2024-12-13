package com.jobprep.resume_feedback.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20); // ## 스레드 풀 크기가 작업 요구사항을 충족하는지 확인
        executor.setMaxPoolSize(50); // ## 동시 처리량에 맞는지 확인
        executor.setQueueCapacity(1000); // ## 작업 대기열이 과부하되는지 확인
        executor.setThreadNamePrefix("AsyncExecutor-");
        executor.initialize();
        return executor;
    }
}