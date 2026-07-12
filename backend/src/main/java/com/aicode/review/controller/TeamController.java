package com.aicode.review.controller;

import com.aicode.review.entity.Team;
import com.aicode.review.entity.TeamMember;
import com.aicode.review.entity.User;
import com.aicode.review.exception.BadRequestException;
import com.aicode.review.exception.ResourceNotFoundException;
import com.aicode.review.repository.TeamMemberRepository;
import com.aicode.review.repository.TeamRepository;
import com.aicode.review.repository.UserRepository;
import com.aicode.review.security.JwtUtil;
import com.aicode.review.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Team Workspaces (bonus feature): simple team creation + membership + shared project visibility. */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestHeader("Authorization") String authHeader,
                                             @RequestBody Map<String, String> body) {
        Long userId = extractUserId(authHeader);
        String name = body.get("name");
        if (name == null || name.isBlank()) throw new BadRequestException("Team name is required");

        Team team = teamRepository.save(Team.builder().name(name).ownerId(userId).build());
        teamMemberRepository.save(TeamMember.builder().teamId(team.getId()).userId(userId).role("OWNER").build());
        return ResponseEntity.ok(team);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> myTeams(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);

        List<Map<String, Object>> result = memberships.stream().map(m -> {
            Team team = teamRepository.findById(m.getTeamId()).orElse(null);
            return Map.<String, Object>of(
                    "teamId", m.getTeamId(),
                    "name", team != null ? team.getName() : "Unknown",
                    "role", m.getRole(),
                    "memberCount", teamMemberRepository.findByTeamId(m.getTeamId()).size()
            );
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<Map<String, String>> addMember(@RequestHeader("Authorization") String authHeader,
                                                            @PathVariable Long teamId,
                                                            @RequestBody Map<String, String> body) {
        Long userId = extractUserId(authHeader);
        requireOwner(teamId, userId);

        String email = body.get("email");
        User invitee = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email " + email));

        if (teamMemberRepository.findByTeamIdAndUserId(teamId, invitee.getId()).isPresent()) {
            throw new BadRequestException("User is already a member of this team");
        }

        teamMemberRepository.save(TeamMember.builder().teamId(teamId).userId(invitee.getId()).role("MEMBER").build());

        Team team = teamRepository.findById(teamId).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        User owner = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        emailService.sendTeamInviteEmail(invitee.getEmail(), team.getName(), owner.getName());

        return ResponseEntity.ok(Map.of("message", "Member added"));
    }

    @DeleteMapping("/{teamId}/members/{memberUserId}")
    public ResponseEntity<Map<String, String>> removeMember(@RequestHeader("Authorization") String authHeader,
                                                               @PathVariable Long teamId,
                                                               @PathVariable Long memberUserId) {
        Long userId = extractUserId(authHeader);
        requireOwner(teamId, userId);

        teamMemberRepository.findByTeamIdAndUserId(teamId, memberUserId)
                .ifPresent(m -> teamMemberRepository.deleteById(m.getId()));

        return ResponseEntity.ok(Map.of("message", "Member removed"));
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<Map<String, Object>>> listMembers(@PathVariable Long teamId) {
        List<Map<String, Object>> members = teamMemberRepository.findByTeamId(teamId).stream()
                .map(m -> {
                    User u = userRepository.findById(m.getUserId()).orElse(null);
                    return Map.<String, Object>of(
                            "userId", m.getUserId(),
                            "name", u != null ? u.getName() : "Unknown",
                            "email", u != null ? u.getEmail() : "",
                            "role", m.getRole()
                    );
                }).toList();
        return ResponseEntity.ok(members);
    }

    private void requireOwner(Long teamId, Long userId) {
        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this team"));
        if (!"OWNER".equals(membership.getRole())) {
            throw new BadRequestException("Only the team owner can perform this action");
        }
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }
}
