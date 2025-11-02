package com.resumeanalyser.resume_analyser_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResumeAnalyserBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(ResumeAnalyserBackendApplication.class, args);
		System.out.println("✅ Resume Analyser Backend is running...");
	}
}
