package com.freelancer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundPaymentRequest {

    private Long projectId;
    private String transactionId;
    private BigDecimal requestedAmount;

    public RefundPaymentRequest() {
    }

    public RefundPaymentRequest(Long projectId, String transactionId, BigDecimal requestedAmount) {
        this.projectId = projectId;
        this.transactionId = transactionId;
        this.requestedAmount = requestedAmount;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }
}
