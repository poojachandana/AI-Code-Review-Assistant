package com.aicode.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewFindingDTO {
    private String severity;
    private String category;
    private String source;
    private String issue;
    private String explanation;
    private String suggestion;
    private String fileName;
    private Integer lineNumber;
}
