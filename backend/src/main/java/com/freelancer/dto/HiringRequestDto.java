package com.freelancer.dto;

import lombok.Data;

@Data
public class HiringRequestDto {
    private Long projectId;
    private Long freelancerId;
    private String message;
}
