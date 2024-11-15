package com.jobprep.resume_feedback;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ResumeFeedbackApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeFeedbackApplication.class, args);
	}

}
