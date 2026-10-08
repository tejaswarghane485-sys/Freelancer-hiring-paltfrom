package com.freelancer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        Long id,
        Long projectId,
        String projectTitle,
        String clientName,
        String freelancerName,
        BigDecimal amount,
        LocalDateTime paymentDate,
        String status,
        String transactionId,
        String paymentMethod,
        LocalDateTime refundDate
) {
}
