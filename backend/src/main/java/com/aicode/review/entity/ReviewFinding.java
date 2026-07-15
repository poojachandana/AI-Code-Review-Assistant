package com.aicode.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_findings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(nullable = false, length = 30)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW, INFO

    @Column(nullable = false, length = 50)
    private String category; // BUG, SECURITY, CODE_SMELL, PERFORMANCE, STYLE, MAINTAINABILITY

    @Column(nullable = false, length = 40)
    private String source; // CHECKSTYLE, PMD, SPOTBUGS, AI

    @Column(columnDefinition = "TEXT")
    private String issue;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "line_number")
    private Integer lineNumber;
}
