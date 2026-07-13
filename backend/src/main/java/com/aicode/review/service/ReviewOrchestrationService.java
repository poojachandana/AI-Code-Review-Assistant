package com.aicode.review.service;

import com.aicode.review.dto.ReviewFindingDTO;
import com.aicode.review.dto.ReviewResponseDTO;
import com.aicode.review.entity.Project;
import com.aicode.review.entity.Review;
import com.aicode.review.entity.ReviewFinding;
import com.aicode.review.entity.User;
import com.aicode.review.exception.ResourceNotFoundException;
import com.aicode.review.repository.ProjectRepository;
import com.aicode.review.repository.ReviewFindingRepository;
import com.aicode.review.repository.ReviewRepository;
import com.aicode.review.repository.UserRepository;
import com.aicode.review.service.analysis.AnalysisFinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewOrchestrationService {

    private final FileProcessingService fileProcessingService;
    private final StaticAnalysisService staticAnalysisService;
    private final ComplexityAnalysisService complexityAnalysisService;
    private final AIReviewService aiReviewService;
    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public ReviewResponseDTO reviewUploadedFile(MultipartFile file, Long userId, String uploadType, Long teamId) throws Exception {
        Path stored = fileProcessingService.storeUpload(file, userId);
        String projectName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "Untitled Project";
        return runPipeline(stored, userId, projectName, uploadType, teamId);
    }

    @Transactional
    public ReviewResponseDTO reviewSnippet(String code, String fileName, String projectName, Long userId, Long teamId) throws Exception {
        Path stored = fileProcessingService.storeSnippet(code, fileName, userId);
        String name = (projectName != null && !projectName.isBlank()) ? projectName : fileName;
        return runPipeline(stored, userId, name, "SNIPPET", teamId);
    }

    private ReviewResponseDTO runPipeline(Path stored, Long userId, String projectName, String uploadType, Long teamId) throws Exception {

        log.info("STEP 1 - Starting pipeline");

        List<Path> sourceFiles = fileProcessingService.extractSupportedSourceFiles(stored);
        log.info("STEP 2 - Source files extracted");

        List<AnalysisFinding> staticFindings = staticAnalysisService.runFullAnalysis(sourceFiles);
        log.info("STEP 3 - Static analysis completed");

        ComplexityAnalysisService.ComplexityResult complexity = complexityAnalysisService.analyze(sourceFiles);
        log.info("STEP 4 - Complexity analysis completed");

        AIReviewService.AiReviewResult aiResult = aiReviewService.review(sourceFiles, staticFindings);
        log.info("STEP 5 - AI review completed");

        List<AnalysisFinding> allFindings = new ArrayList<>(staticFindings);
        allFindings.addAll(aiResult.findings());

        int qualityScore = (aiResult.available() && aiResult.qualityScore() != null)
                ? aiResult.qualityScore()
                : staticAnalysisService.computeQualityScore(staticFindings, complexity);

        String summary = aiResult.available()
                ? aiResult.summary()
                : buildFallbackSummary(staticFindings, complexity, aiResult);

        Project project = Project.builder()
                .userId(userId)
                .projectName(projectName)
                .uploadType(uploadType)
                .storagePath(stored.toString())
                .teamId(teamId)
                .build();

        project = projectRepository.save(project);
        log.info("STEP 6 - Project saved with ID {}", project.getId());

        Review review = Review.builder()
                .projectId(project.getId())
                .reviewScore(qualityScore)
                .summary(summary)
                .numClasses(complexity.getNumClasses())
                .numMethods(complexity.getNumMethods())
                .linesOfCode(complexity.getLinesOfCode())
                .avgMethodLength(complexity.getAvgMethodLength())
                .cyclomaticComplexity(complexity.getCyclomaticComplexity())
                .maintainabilityIndex(complexity.getMaintainabilityIndex())
                .build();

        review = reviewRepository.save(review);
        log.info("STEP 7 - Review saved with ID {}", review.getId());

        // your findings loop...

        log.info("STEP 8 - Findings saved");

        ReviewResponseDTO responseDTO = ReviewResponseDTO.builder()
                // existing builder...
                .build();

        log.info("STEP 9 - Response created");

        userRepository.findById(userId).ifPresent(user ->
                emailService.sendReviewCompleteEmail(user, responseDTO));

        log.info("STEP 10 - Returning response");

        return responseDTO;
    }

    private String buildFallbackSummary(List<AnalysisFinding> staticFindings,
                                         ComplexityAnalysisService.ComplexityResult complexity,
                                         AIReviewService.AiReviewResult aiResult) {
        long critical = staticFindings.stream().filter(f -> "CRITICAL".equals(f.getSeverity())).count();
        long high = staticFindings.stream().filter(f -> "HIGH".equals(f.getSeverity())).count();
        return String.format(
                "Static analysis found %d finding(s) (%d critical, %d high). Maintainability index: %.1f/100. %s",
                staticFindings.size(), critical, high, complexity.getMaintainabilityIndex(), aiResult.summary());
    }

    public ReviewResponseDTO getReviewDetail(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        Project project = projectRepository.findById(review.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<ReviewFindingDTO> findings = reviewFindingRepository.findByReviewId(reviewId).stream()
                .map(f -> ReviewFindingDTO.builder()
                        .severity(f.getSeverity())
                        .category(f.getCategory())
                        .source(f.getSource())
                        .issue(f.getIssue())
                        .explanation(f.getExplanation())
                        .suggestion(f.getSuggestion())
                        .fileName(f.getFileName())
                        .lineNumber(f.getLineNumber())
                        .build())
                .toList();

        return ReviewResponseDTO.builder()
                .reviewId(review.getId())
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .reviewScore(review.getReviewScore())
                .summary(review.getSummary())
                .numClasses(review.getNumClasses())
                .numMethods(review.getNumMethods())
                .linesOfCode(review.getLinesOfCode())
                .avgMethodLength(review.getAvgMethodLength())
                .cyclomaticComplexity(review.getCyclomaticComplexity())
                .maintainabilityIndex(review.getMaintainabilityIndex())
                .createdAt(review.getCreatedAt())
                .findings(findings)
                .build();
    }
}
