package com.jobprep.resume_feedback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableCaching
@SpringBootApplication(exclude = {RestTemplateAutoConfiguration.class})
public class ResumeFeedbackApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeFeedbackApplication.class, args);
	}

}
