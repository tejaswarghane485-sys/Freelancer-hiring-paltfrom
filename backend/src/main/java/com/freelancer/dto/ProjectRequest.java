package com.freelancer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String description;
    private String requiredSkills;
    private BigDecimal budget;
    private String deadline; // ISO date string yyyy-MM-dd
    private String status;
}
