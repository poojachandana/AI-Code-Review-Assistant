package com.aicode.review.controller;

import com.aicode.review.dto.ProjectSummaryDTO;
import com.aicode.review.dto.ReviewResponseDTO;
import com.aicode.review.dto.SnippetSubmissionRequest;
import com.aicode.review.entity.Project;
import com.aicode.review.exception.ResourceNotFoundException;
import com.aicode.review.repository.ProjectRepository;
import com.aicode.review.repository.ReviewRepository;
import com.aicode.review.security.JwtUtil;
import com.aicode.review.service.ReviewOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ReviewOrchestrationService reviewOrchestrationService;
    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final JwtUtil jwtUtil;
    private final com.aicode.review.service.FileProcessingService fileProcessingService;
    private final com.aicode.review.service.AIReviewService aiReviewService;

    /** Code Submission: upload one or more source files or a ZIP archive. */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ReviewResponseDTO> upload(@RequestHeader("Authorization") String authHeader,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "teamId", required = false) Long teamId) throws Exception {
        Long userId = extractUserId(authHeader);
        String uploadType = file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".zip")
                ? "ZIP" : "FILE";
        ReviewResponseDTO result = reviewOrchestrationService.reviewUploadedFile(file, userId, uploadType, teamId);
        return ResponseEntity.ok(result);
    }

    /** Code Submission: paste a code snippet directly. */
    @PostMapping("/snippet")
    public ResponseEntity<ReviewResponseDTO> submitSnippet(@RequestHeader("Authorization") String authHeader,
                                                             @Valid @RequestBody SnippetSubmissionRequest request) throws Exception {
        Long userId = extractUserId(authHeader);
        ReviewResponseDTO result = reviewOrchestrationService.reviewSnippet(
                request.getCode(), request.getFileName(), request.getProjectName(), userId, request.getTeamId());
        return ResponseEntity.ok(result);
    }

    /** Review Dashboard: list all previous projects/reviews for the logged-in user. */
    @GetMapping
    public ResponseEntity<List<ProjectSummaryDTO>> listProjects(@RequestHeader("Authorization") String authHeader,
                                                                  @RequestParam(required = false) String search) {
        Long userId = extractUserId(authHeader);
        List<Project> projects = projectRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(toSummaries(projects, search));
    }

    /** Team workspace: list projects submitted under a given team. */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<ProjectSummaryDTO>> listTeamProjects(@PathVariable Long teamId,
                                                                      @RequestParam(required = false) String search) {
        List<Project> projects = projectRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
        return ResponseEntity.ok(toSummaries(projects, search));
    }

    /** Lists the source file names for a project - used by the AI Refactor panel. */
    @GetMapping("/{projectId}/files")
    public ResponseEntity<List<String>> listProjectFiles(@PathVariable Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        List<java.nio.file.Path> files = fileProcessingService.extractSupportedSourceFiles(
                java.nio.file.Path.of(project.getStoragePath()));
        return ResponseEntity.ok(files.stream().map(p -> p.getFileName().toString()).toList());
    }

    private List<ProjectSummaryDTO> toSummaries(List<Project> projects, String search) {
        return projects.stream()
                .filter(p -> search == null || search.isBlank()
                        || p.getProjectName().toLowerCase().contains(search.toLowerCase()))
                .map(p -> {
                    var latest = reviewRepository.findTopByProjectIdOrderByCreatedAtDesc(p.getId());
                    return ProjectSummaryDTO.builder()
                            .projectId(p.getId())
                            .projectName(p.getProjectName())
                            .uploadType(p.getUploadType())
                            .createdAt(p.getCreatedAt())
                            .latestReviewId(latest.map(r -> r.getId()).orElse(null))
                            .latestScore(latest.map(r -> r.getReviewScore()).orElse(null))
                            .build();
                })
                .toList();
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Map<String, String>> deleteProject(@RequestHeader("Authorization") String authHeader,
                                                                @PathVariable Long projectId) {
        Long userId = extractUserId(authHeader);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!project.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Project not found");
        }

        reviewRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .forEach(r -> reviewRepository.deleteById(r.getId()));
        projectRepository.deleteById(projectId);

        return ResponseEntity.ok(Map.of("message", "Project deleted"));
    }

    /** AI-powered code refactoring (bonus feature): rewrite one file per AI best practices. */
    @PostMapping("/{projectId}/refactor")
    public ResponseEntity<Map<String, Object>> refactorFile(@PathVariable Long projectId,
                                                              @RequestParam("fileName") String fileName) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<java.nio.file.Path> files = fileProcessingService.extractSupportedSourceFiles(
                java.nio.file.Path.of(project.getStoragePath()));

        java.nio.file.Path target = files.stream()
                .filter(p -> p.getFileName().toString().equals(fileName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found in project: " + fileName));

        var result = aiReviewService.refactorFile(target);

        return ResponseEntity.ok(Map.of(
                "fileName", fileName,
                "available", result.available(),
                "refactoredCode", result.refactoredCode() != null ? result.refactoredCode() : "",
                "error", result.error() != null ? result.error() : ""
        ));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }
}
