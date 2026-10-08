package com.freelancer.dto;

import jakarta.validation.constraints.NotNull;
import com.freelancer.model.Payment;
import lombok.Data;

@Data
public class PayProjectRequest {
    @NotNull
    private Long projectId;

    private Payment.Method paymentMethod = Payment.Method.UPI;
}
