package com.aicode.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SnippetSubmissionRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String fileName; // e.g. "Main.java"

    private String projectName;

    private Long teamId;
}
