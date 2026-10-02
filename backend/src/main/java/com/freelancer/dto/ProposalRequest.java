package com.freelancer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProposalRequest {
    private Long projectId;
    @NotBlank
    private String coverLetter;
    private BigDecimal proposedBudget;
    private Integer estimatedDays;
}
