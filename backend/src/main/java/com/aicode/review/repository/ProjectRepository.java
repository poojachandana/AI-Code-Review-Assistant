package com.aicode.review.repository;

import com.aicode.review.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Project> findByTeamIdOrderByCreatedAtDesc(Long teamId);
}
