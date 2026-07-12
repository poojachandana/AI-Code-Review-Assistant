package com.aicode.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "upload_type", nullable = false)
    private String uploadType; // FILE, ZIP, SNIPPET

    @Column(name = "storage_path")
    private String storagePath;

    /** Nullable - set when the project was submitted under a team workspace. */
    @Column(name = "team_id")
    private Long teamId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
