package com.aicode.review.service.analysis;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.reporting.RuleViolation;
import net.sourceforge.pmd.reporting.Report;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the PMD 7 Java API to detect code smells, best-practice violations,
 * and design issues across the standard PMD rule categories.
 */
@Slf4j
@Component
public class PmdAnalyzer {

    private static final String[] RULESETS = {
            "category/java/bestpractices.xml",
            "category/java/errorprone.xml",
            "category/java/design.xml",
            "category/java/performance.xml",
            "category/java/security.xml",
            "category/java/multithreading.xml"
    };

    public List<AnalysisFinding> analyze(List<Path> javaFiles) {
        List<AnalysisFinding> findings = new ArrayList<>();

        PMDConfiguration config = new PMDConfiguration();
        for (String ruleset : RULESETS) {
            config.addRuleSet(ruleset);
        }

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            for (Path file : javaFiles) {
                pmd.files().addFile(file);
            }

            Report report = pmd.performAnalysisAndCollectReport();

            for (RuleViolation violation : report.getViolations()) {
                findings.add(AnalysisFinding.builder()
                        .source("PMD")
                        .category(mapCategory(violation.getRule().getName()))
                        .severity(mapSeverity(violation.getRule().getPriority().getPriority()))
                        .fileName(new File(violation.getFileId().getOriginalPath()).getName())
                        .lineNumber(violation.getBeginLine())
                        .issue(violation.getDescription())
                        .explanation("PMD rule '" + violation.getRule().getName() + "' flagged this in category '"
                                + violation.getRule().getRuleSetName() + "'.")
                        .suggestion("Refactor according to the '" + violation.getRule().getName() + "' guideline.")
                        .build());
            }
        } catch (Exception e) {
            log.error("PMD analysis failed: {}", e.getMessage(), e);
            findings.add(AnalysisFinding.builder()
                    .source("PMD")
                    .category("CODE_SMELL")
                    .severity("INFO")
                    .issue("PMD analysis could not complete")
                    .explanation(e.getMessage())
                    .suggestion("Verify the uploaded files are valid, parseable Java source.")
                    .build());
        }

        return findings;
    }

    private String mapSeverity(int pmdPriority) {
        // PMD priority: 1 (highest) .. 5 (lowest)
        return switch (pmdPriority) {
            case 1 -> "CRITICAL";
            case 2 -> "HIGH";
            case 3 -> "MEDIUM";
            case 4 -> "LOW";
            default -> "INFO";
        };
    }

    private String mapCategory(String ruleName) {
        String lower = ruleName.toLowerCase();
        if (lower.contains("security")) return "SECURITY";
        if (lower.contains("perf")) return "PERFORMANCE";
        if (lower.contains("empty") || lower.contains("unused") || lower.contains("duplicat")) return "CODE_SMELL";
        return "CODE_SMELL";
    }
}
