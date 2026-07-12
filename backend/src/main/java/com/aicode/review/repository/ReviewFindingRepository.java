package com.aicode.review.repository;

import com.aicode.review.entity.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, Long> {
    List<ReviewFinding> findByReviewId(Long reviewId);
    List<ReviewFinding> findByReviewIdAndSeverity(Long reviewId, String severity);
}
