package com.aicode.review.service;

import com.aicode.review.service.analysis.AnalysisFinding;
import com.aicode.review.service.analysis.BugPatternAnalyzer;
import com.aicode.review.service.analysis.CheckstyleAnalyzer;
import com.aicode.review.service.analysis.PmdAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 2: Static Code Analysis.
 * Runs Checkstyle (coding standards), PMD (code smells/best practices) and a
 * SpotBugs-style bug-pattern detector, then merges the results.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaticAnalysisService {

    private final CheckstyleAnalyzer checkstyleAnalyzer;
    private final PmdAnalyzer pmdAnalyzer;
    private final BugPatternAnalyzer bugPatternAnalyzer;

    public List<AnalysisFinding> runFullAnalysis(List<Path> javaFiles) {
        List<AnalysisFinding> allFindings = new ArrayList<>();

        List<Path> onlyJava = javaFiles.stream()
                .filter(p -> p.getFileName().toString().endsWith(".java"))
                .toList();

        if (onlyJava.isEmpty()) {
            log.info("No .java files found; skipping Java-specific static analysis (Checkstyle/PMD/SpotBugs-lite).");
            return allFindings;
        }

        try {
            allFindings.addAll(checkstyleAnalyzer.analyze(onlyJava));
        } catch (Exception e) {
            log.error("Checkstyle stage failed: {}", e.getMessage());
        }

        try {
            allFindings.addAll(pmdAnalyzer.analyze(onlyJava));
        } catch (Exception e) {
            log.error("PMD stage failed: {}", e.getMessage());
        }

        try {
            allFindings.addAll(bugPatternAnalyzer.analyze(onlyJava));
        } catch (Exception e) {
            log.error("Bug pattern (SpotBugs-lite) stage failed: {}", e.getMessage());
        }

        return allFindings;
    }

    /** Compute an overall 0-100 code quality score from static findings + complexity. */
    public int computeQualityScore(List<AnalysisFinding> findings, ComplexityAnalysisService.ComplexityResult complexity) {
        double score = 100.0;

        for (AnalysisFinding f : findings) {
            score -= switch (f.getSeverity()) {
                case "CRITICAL" -> 8.0;
                case "HIGH" -> 4.0;
                case "MEDIUM" -> 2.0;
                case "LOW" -> 0.75;
                default -> 0.25;
            };
        }

        score -= Math.max(0, (10 - complexity.getMaintainabilityIndex() / 10));
        score -= Math.max(0, (complexity.getCyclomaticComplexity() - 10) * 0.5);

        return (int) Math.round(Math.max(0, Math.min(100, score)));
    }
}
