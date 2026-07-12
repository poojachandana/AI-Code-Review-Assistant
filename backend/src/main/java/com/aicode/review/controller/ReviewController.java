package com.aicode.review.controller;

import com.aicode.review.dto.ReviewResponseDTO;
import com.aicode.review.entity.Project;
import com.aicode.review.exception.ResourceNotFoundException;
import com.aicode.review.repository.ProjectRepository;
import com.aicode.review.repository.ReviewRepository;
import com.aicode.review.service.DocumentationGeneratorService;
import com.aicode.review.service.FileProcessingService;
import com.aicode.review.service.PdfReportService;
import com.aicode.review.service.ReviewOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewOrchestrationService reviewOrchestrationService;
    private final PdfReportService pdfReportService;
    private final DocumentationGeneratorService documentationGeneratorService;
    private final FileProcessingService fileProcessingService;
    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;

    /** Review Dashboard: view a detailed report for one review. */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> getReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewOrchestrationService.getReviewDetail(reviewId));
    }

    /** Review history for a given project. */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsForProject(@PathVariable Long projectId) {
        List<ReviewResponseDTO> reviews = reviewRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(r -> reviewOrchestrationService.getReviewDetail(r.getId()))
                .toList();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping(value = "/{reviewId}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long reviewId) throws Exception {
        ReviewResponseDTO review = reviewOrchestrationService.getReviewDetail(reviewId);
        byte[] pdf = pdfReportService.generatePdf(review);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("review-" + reviewId + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/{reviewId}/export/html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> exportHtml(@PathVariable Long reviewId) {
        ReviewResponseDTO review = reviewOrchestrationService.getReviewDetail(reviewId);
        return ResponseEntity.ok(pdfReportService.generateHtml(review));
    }

    @GetMapping(value = "/{reviewId}/export/markdown", produces = "text/markdown")
    public ResponseEntity<String> exportMarkdown(@PathVariable Long reviewId) {
        ReviewResponseDTO review = reviewOrchestrationService.getReviewDetail(reviewId);
        String markdown = pdfReportService.generateMarkdown(review);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("review-" + reviewId + ".md").build().toString())
                .body(markdown);
    }

    /** Documentation Generator feature: class/method docs + API doc + README summary. */
    @GetMapping(value = "/project/{projectId}/documentation", produces = "text/markdown")
    public ResponseEntity<String> generateDocumentation(@PathVariable Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Path storedPath = Path.of(project.getStoragePath());
        List<Path> sourceFiles = fileProcessingService.extractSupportedSourceFiles(storedPath);

        String docs = documentationGeneratorService.generateMarkdownDocs(sourceFiles, project.getProjectName());
        return ResponseEntity.ok(docs);
    }

    @GetMapping(value = "/project/{projectId}/api-docs", produces = "text/markdown")
    public ResponseEntity<String> generateApiDocs(@PathVariable Long projectId) throws Exception {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Path storedPath = Path.of(project.getStoragePath());
        List<Path> sourceFiles = fileProcessingService.extractSupportedSourceFiles(storedPath);

        return ResponseEntity.ok(documentationGeneratorService.generateApiDocSummary(sourceFiles));
    }

    /** Documentation Generator feature: README Summary (Optional), based on the latest review's metrics. */
    @GetMapping(value = "/project/{projectId}/readme-summary", produces = "text/markdown")
    public ResponseEntity<String> generateReadmeSummary(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        var latestReview = reviewRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("No review found for this project yet"));

        String readme = documentationGeneratorService.generateReadmeSummary(
                project.getProjectName(),
                latestReview.getNumClasses() != null ? latestReview.getNumClasses() : 0,
                latestReview.getNumMethods() != null ? latestReview.getNumMethods() : 0,
                latestReview.getLinesOfCode() != null ? latestReview.getLinesOfCode() : 0,
                latestReview.getReviewScore() != null ? latestReview.getReviewScore() : 0
        );
        return ResponseEntity.ok(readme);
    }
}
