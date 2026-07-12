package com.aicode.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryDTO {
    private Long projectId;
    private String projectName;
    private String uploadType;
    private LocalDateTime createdAt;
    private Long latestReviewId;
    private Integer latestScore;
}
