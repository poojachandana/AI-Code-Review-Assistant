package com.aicode.review.repository;

import com.aicode.review.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByUserId(Long userId);
    List<TeamMember> findByTeamId(Long teamId);
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);
}
