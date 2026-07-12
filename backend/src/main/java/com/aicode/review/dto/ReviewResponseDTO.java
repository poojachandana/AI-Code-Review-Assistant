package com.aicode.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponseDTO {
    private Long reviewId;
    private Long projectId;
    private String projectName;
    private Integer reviewScore;
    private String summary;

    private Integer numClasses;
    private Integer numMethods;
    private Integer linesOfCode;
    private Double avgMethodLength;
    private Double cyclomaticComplexity;
    private Double maintainabilityIndex;

    private LocalDateTime createdAt;
    private List<ReviewFindingDTO> findings;
}
