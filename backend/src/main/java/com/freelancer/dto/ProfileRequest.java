package com.freelancer.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfileRequest {
    private String bio;
    private String skills;
    private String experience;
    private String education;
    private String portfolioUrl;
    private String phone;
    private String location;
    private BigDecimal hourlyRate;
}
