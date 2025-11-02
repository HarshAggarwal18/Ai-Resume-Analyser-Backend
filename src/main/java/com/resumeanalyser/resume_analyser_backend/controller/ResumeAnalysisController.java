package com.resumeanalyser.resume_analyser_backend.controller;

import com.resumeanalyser.resume_analyser_backend.dto.*;
import com.resumeanalyser.resume_analyser_backend.model.Job;
import com.resumeanalyser.resume_analyser_backend.service.AIAnalyzerService;
import com.resumeanalyser.resume_analyser_backend.service.JobService;
import com.resumeanalyser.resume_analyser_backend.service.ResumeParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/analyze")
public class ResumeAnalysisController {

    private final ResumeParserService resumeParserService;
    private final JobService jobService;
    private final AIAnalyzerService aiAnalyzerService;

    public ResumeAnalysisController(
            ResumeParserService resumeParserService,
            JobService jobService,
            AIAnalyzerService aiAnalyzerService
    ) {
        this.resumeParserService = resumeParserService;
        this.jobService = jobService;
        this.aiAnalyzerService = aiAnalyzerService;
    }

    @PostMapping("/resume")
    public ResponseEntity<List<AnalysisResultDTO>> analyzeResume(@RequestParam("file") MultipartFile file) {
        try {
            // 1️⃣ Extract Resume
            String text = resumeParserService.extractTextFromResume(file);
            ResumeDTO resume = new ResumeDTO("Unknown", "N/A", text);

            // 2️⃣ Fetch Jobs
            List<Job> jobs = jobService.getAllJobs();

            // 3️⃣ Prepare DTOs and Analyze
            List<AnalysisResultDTO> results = new ArrayList<>();
            for (Job job : jobs) {
                JobDTO jobDTO = new JobDTO(
                        job.getId(),
                        job.getJobTitle(),
                        job.getCompany(),
                        job.getLocation(),
                        job.getJobType(),
                        job.getExperience(),
                        job.getDescription(),
                        job.getSkillsRequired()
                );

                AIRequestDTO request = new AIRequestDTO(resume, jobDTO);
                AnalysisResultDTO result = aiAnalyzerService.analyze(request);
                results.add(result);
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
