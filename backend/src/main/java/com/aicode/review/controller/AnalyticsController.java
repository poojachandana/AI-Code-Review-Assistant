package com.aicode.review.controller;

import com.aicode.review.entity.Project;
import com.aicode.review.entity.Review;
import com.aicode.review.entity.ReviewFinding;
import com.aicode.review.repository.ProjectRepository;
import com.aicode.review.repository.ReviewFindingRepository;
import com.aicode.review.repository.ReviewRepository;
import com.aicode.review.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Repository Analytics Dashboard (bonus feature): aggregate insight across all of a user's projects. */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        List<Project> projects = projectRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Review> allReviews = projects.stream()
                .flatMap(p -> reviewRepository.findByProjectIdOrderByCreatedAtDesc(p.getId()).stream())
                .sorted(Comparator.comparing(Review::getCreatedAt))
                .toList();

        double avgScore = allReviews.stream()
                .filter(r -> r.getReviewScore() != null)
                .mapToInt(Review::getReviewScore)
                .average()
                .orElse(0);

        List<Map<String, Object>> scoreTrend = allReviews.stream()
                .map(r -> Map.<String, Object>of(
                        "date", r.getCreatedAt().toLocalDate().toString(),
                        "score", r.getReviewScore()
                ))
                .toList();

        List<ReviewFinding> allFindings = allReviews.stream()
                .flatMap(r -> reviewFindingRepository.findByReviewId(r.getId()).stream())
                .toList();

        Map<String, Long> severityDistribution = allFindings.stream()
                .collect(Collectors.groupingBy(ReviewFinding::getSeverity, Collectors.counting()));

        Map<String, Long> categoryDistribution = allFindings.stream()
                .collect(Collectors.groupingBy(ReviewFinding::getCategory, Collectors.counting()));

        List<Map<String, Object>> topCategories = categoryDistribution.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> Map.<String, Object>of("category", e.getKey(), "count", e.getValue()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "totalProjects", projects.size(),
                "totalReviews", allReviews.size(),
                "averageQualityScore", Math.round(avgScore * 10.0) / 10.0,
                "scoreTrend", scoreTrend,
                "severityDistribution", severityDistribution,
                "topCategories", topCategories
        ));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }
}
