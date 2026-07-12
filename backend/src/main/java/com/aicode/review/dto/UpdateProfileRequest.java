package com.aicode.review.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String currentPassword;
    private String newPassword;
    private Boolean emailNotifications;
}
