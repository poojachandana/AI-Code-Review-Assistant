package com.aicode.review.service.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Common shape returned by every static analyzer + the AI reviewer before persistence. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisFinding {
    private String severity;     // CRITICAL, HIGH, MEDIUM, LOW, INFO
    private String category;     // BUG, SECURITY, CODE_SMELL, PERFORMANCE, STYLE, MAINTAINABILITY
    private String source;       // CHECKSTYLE, PMD, SPOTBUGS, AI
    private String issue;
    private String explanation;
    private String suggestion;
    private String fileName;
    private Integer lineNumber;
}
