package com.freelancer.dto;

import com.freelancer.model.Project;

import java.math.BigDecimal;

public record ProjectResponse(
        Long id,
        String title,
        String clientName,
        String freelancerName,
        BigDecimal clientBudget,
        BigDecimal freelancerPrice,
        Project.Status status) {
}
