package com.freelancer.dto;

import com.freelancer.model.HiringRequest;
import com.freelancer.model.Project;
import com.freelancer.model.User;

import java.time.LocalDateTime;

public record HiringRequestResponse(
        Long id,
        Project project,
        User client,
        User freelancer,
        String message,
        HiringRequest.Status status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static HiringRequestResponse from(HiringRequest request) {
        return new HiringRequestResponse(
                request.getId(),
                request.getProject(),
                request.getClient(),
                request.getFreelancer(),
                request.getMessage(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
