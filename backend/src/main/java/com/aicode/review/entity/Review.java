package com.aicode.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "review_score")
    private Integer reviewScore; // Code Quality Score 0-100

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // Complexity metrics
    private Integer numClasses;
    private Integer numMethods;
    private Integer linesOfCode;
    private Double avgMethodLength;
    private Double cyclomaticComplexity;
    private Double maintainabilityIndex;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
