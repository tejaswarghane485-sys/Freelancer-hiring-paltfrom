package com.freelancer.dto;

import java.math.BigDecimal;
import java.util.List;

public record FreelancerEarningsResponse(
        List<PaymentTransactionResponse> payments,
        BigDecimal totalEarnings
) {
}
