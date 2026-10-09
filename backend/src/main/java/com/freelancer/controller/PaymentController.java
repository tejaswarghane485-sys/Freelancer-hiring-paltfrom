package com.freelancer.controller;

import com.freelancer.dto.PayProjectRequest;
import com.freelancer.dto.FreelancerEarningsResponse;
import com.freelancer.dto.RefundPaymentRequest;
import com.freelancer.dto.RefundDecisionRequest;
import com.freelancer.dto.PaymentTransactionResponse;
import com.freelancer.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final String USER_ID_ATTRIBUTE = "userId";

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/client")
    public ResponseEntity<List<PaymentTransactionResponse>> getClientTransactions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(USER_ID_ATTRIBUTE);
        return ResponseEntity.ok(paymentService.getClientTransactions(userId));
    }

    @GetMapping("/freelancer")
    public ResponseEntity<FreelancerEarningsResponse> getFreelancerEarnings(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(USER_ID_ATTRIBUTE);
        return ResponseEntity.ok(paymentService.getFreelancerEarnings(userId));
    }

    @PostMapping("/pay")
    public ResponseEntity<Object> pay(@Valid @RequestBody PayProjectRequest payment,
                                      HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute(USER_ID_ATTRIBUTE);
            return ResponseEntity.ok(paymentService.pay(userId, payment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{projectId}/refund")
    public ResponseEntity<Object> refund(@PathVariable long projectId,
                                         HttpServletRequest request) {
        return refund(new RefundPaymentRequest(projectId, null, null), request);
    }

    @PostMapping("/refund")
    public ResponseEntity<Object> refund(@RequestBody RefundPaymentRequest payment,
                                         HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute(USER_ID_ATTRIBUTE);
            return ResponseEntity.ok(paymentService.refund(
                    userId, payment.getProjectId(), payment.getTransactionId(), payment.getRequestedAmount()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{projectId}/refund/submit")
    public ResponseEntity<Object> submitRefund(@PathVariable long projectId,
                                               @RequestBody RefundDecisionRequest refund,
                                               HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute(USER_ID_ATTRIBUTE);
            return ResponseEntity.ok(paymentService.submitRefund(userId, projectId, refund.amount()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{projectId}/refund/reject")
    public ResponseEntity<Object> rejectRefund(@PathVariable long projectId, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute(USER_ID_ATTRIBUTE);
            return ResponseEntity.ok(paymentService.rejectRefund(userId, projectId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
