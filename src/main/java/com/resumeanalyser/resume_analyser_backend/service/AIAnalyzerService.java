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
You are a precise AI Resume–Job Matching Assistant.

OUTPUT RULES:
- Return ONLY valid JSON. Start with "{" and end with "}". No markdown, comments, or extra text.
- Use EXACTLY the fields and order shown in the schema below. Do not add or rename fields.
- Every scalar field MUST be non-empty. If unknown, use "Unknown" (never null).
- Arrays may be empty [] if nothing fits; otherwise include 3–12 concise items.
- Numbers are floats in [0.0, 100.0] with at most 1 decimal (e.g., 87.5).
- Keep each paragraph ≤ 70 words.

NORMALIZATION:
- employmentType ∈ ["Full-time","Part-time","Contract","Internship","Temporary","Volunteer","Apprenticeship","Remote","Freelance","Unknown"].
- location format: "City, Country" if possible; else "Country"; else "Remote"; else "Unknown".
- Prefer job title/company if present; otherwise infer from resume; else "Unknown".
- Lists must be unique and relevance-sorted.

SCORING:
- skillsMatch weight 0.50, experienceMatch weight 0.30, educationMatch weight 0.20.
- overall = 0.5*skillsMatch + 0.3*experienceMatch + 0.2*educationMatch (round to 1 decimal).

STRICT JSON SCHEMA (use this exact order):
{
  "title": "string",
  "company": "string",
  "location": "string",
  "employmentType": "string",
  "matchScore": {
    "overall": 0.0,
    "skillsMatch": 0.0,
    "experienceMatch": 0.0,
    "educationMatch": 0.0
  },
  "matchingSkills": ["string"],
  "missingSkills": ["string"],
  "whyFit": "string",
  "growthAreas": "string",
  "summary": "string"
}

INPUTS — analyze carefully and then output ONLY the JSON:

--- RESUME ---
Name: %s
Email: %s
Text:
%s

--- JOB ---
Title: %s
Company: %s
Location: %s
Type: %s
Experience: %s
Description:
%s
Required Skills: %s
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
