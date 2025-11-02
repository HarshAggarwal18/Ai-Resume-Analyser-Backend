package com.resumeanalyser.resume_analyser_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyser.resume_analyser_backend.dto.AIRequestDTO;
import com.resumeanalyser.resume_analyser_backend.dto.AnalysisResultDTO;
import com.resumeanalyser.resume_analyser_backend.dto.JobDTO;
import com.resumeanalyser.resume_analyser_backend.dto.ResumeDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AIAnalyzerService {

    private final ChatClient chatClient;

    public AIAnalyzerService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    public AnalysisResultDTO analyze(AIRequestDTO dto) {
        String prompt = buildPrompt(dto.getResume(), dto.getJob());
        String rawResponse = chatClient.prompt(prompt).call().content(); // get raw string

        String fixedJson = sanitizeJson(rawResponse);

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(fixedJson, AnalysisResultDTO.class);
        } catch (Exception e) {
            // If parsing still fails, wrap minimal info
            AnalysisResultDTO fallback = new AnalysisResultDTO();
            fallback.setSummary("❌ Failed to parse AI output: " + e.getMessage());
            return fallback;
        }
    }

    private String sanitizeJson(String raw) {
        if (raw == null) return "{}";

        // Trim non-JSON text (AI often adds “Here’s your JSON:” etc.)
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return raw.substring(start, end + 1);
        }

        // If no clear braces, wrap whole response
        return "{ \"summary\": \"" + raw.replace("\"", "'") + "\" }";
    }

//    public AnalysisResultDTO analyze(AIRequestDTO dto) {
//        String prompt = buildPrompt(dto.getResume(), dto.getJob());
//        return chatClient
//                .prompt(prompt)
//                .call()
//                .entity(AnalysisResultDTO.class);
//    }

    private String buildPrompt(ResumeDTO resume, JobDTO job) {
        return """
        You are an AI resume–job matching assistant.
        Your response **must be valid JSON only** — no explanations, no markdown, no text before or after.
        Follow this JSON schema exactly:

        {
          "title": "",
          "company": "",
          "location": "",
          "employmentType": "",
          "matchScore": {
            "overall": 0.0,
            "skillsMatch": 0.0,
            "experienceMatch": 0.0,
            "educationMatch": 0.0
          },
          "matchingSkills": [],
          "missingSkills": [],
          "whyFit": "",
          "growthAreas": "",
          "summary": ""
        }

        Now analyze the following:

        RESUME:
        Name: %s
        Email: %s
        Text: %s

        JOB:
        Title: %s
        Company: %s
        Location: %s
        Type: %s
        Experience: %s
        Description: %s
        Skills: %s
        """.formatted(
                resume.getName(),
                resume.getEmail(),
                resume.getText(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getJobType(),
                job.getExperience(),
                job.getDescription(),
                String.join(", ", job.getSkillsRequired())
        );
    }

}
