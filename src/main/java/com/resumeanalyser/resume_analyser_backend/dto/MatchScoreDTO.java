package com.resumeanalyser.resume_analyser_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchScoreDTO {
    private double overall;
    private double skillsMatch;
    private double experienceMatch;
    private double educationMatch;
}
