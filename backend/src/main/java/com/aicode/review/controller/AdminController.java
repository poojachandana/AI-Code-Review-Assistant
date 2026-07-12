package com.aicode.review.controller;

import com.aicode.review.entity.Project;
import com.aicode.review.entity.Review;
import com.aicode.review.entity.User;
import com.aicode.review.repository.ProjectRepository;
import com.aicode.review.repository.ReviewFindingRepository;
import com.aicode.review.repository.ReviewRepository;
import com.aicode.review.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Dashboard (bonus feature): platform-wide visibility for administrators.
 * The very first registered user, or any email in app.admin.emails, is granted
 * ROLE_ADMIN (see AuthService). All endpoints here require that role.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewFindingRepository reviewFindingRepository;

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "name", u.getName(),
                        "email", u.getEmail(),
                        "role", u.getRole(),
                        "createdAt", u.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        List<Project> projects = projectRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Project p : projects) {
            reviewRepository.findByProjectIdOrderByCreatedAtDesc(p.getId())
                    .forEach(r -> reviewRepository.deleteById(r.getId()));
            projectRepository.deleteById(p.getId());
        }
        userRepository.deleteById(userId);
        return ResponseEntity.ok(Map.of("message", "User and their projects deleted"));
    }

    @GetMapping("/projects")
    public ResponseEntity<List<Map<String, Object>>> listAllProjects() {
        List<Map<String, Object>> projects = projectRepository.findAll().stream()
                .map(p -> {
                    User owner = userRepository.findById(p.getUserId()).orElse(null);
                    var latest = reviewRepository.findTopByProjectIdOrderByCreatedAtDesc(p.getId());
                    return Map.<String, Object>of(
                            "projectId", p.getId(),
                            "projectName", p.getProjectName(),
                            "ownerEmail", owner != null ? owner.getEmail() : "unknown",
                            "uploadType", p.getUploadType(),
                            "createdAt", p.getCreatedAt(),
                            "latestScore", latest.map(Review::getReviewScore).orElse(null)
                    );
                })
                .toList();
        return ResponseEntity.ok(projects);
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Map<String, String>> deleteProject(@PathVariable Long projectId) {
        reviewRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .forEach(r -> reviewRepository.deleteById(r.getId()));
        projectRepository.deleteById(projectId);
        return ResponseEntity.ok(Map.of("message", "Project deleted"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> platformStats() {
        long totalUsers = userRepository.count();
        long totalProjects = projectRepository.count();
        List<Review> allReviews = reviewRepository.findAll();
        long totalReviews = allReviews.size();

        double avgScore = allReviews.stream()
                .filter(r -> r.getReviewScore() != null)
                .mapToInt(Review::getReviewScore)
                .average()
                .orElse(0);

        Map<String, Long> severityBreakdown = reviewFindingRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        com.aicode.review.entity.ReviewFinding::getSeverity,
                        Collectors.counting()));

        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "totalProjects", totalProjects,
                "totalReviews", totalReviews,
                "averageQualityScore", Math.round(avgScore * 10.0) / 10.0,
                "severityBreakdown", severityBreakdown
        ));
    }
}
