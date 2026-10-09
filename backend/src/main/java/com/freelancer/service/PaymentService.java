package com.freelancer.service;

import com.freelancer.dto.FreelancerEarningsResponse;
import com.freelancer.dto.PayProjectRequest;
import com.freelancer.dto.PaymentTransactionResponse;
import com.freelancer.model.Payment;
import com.freelancer.model.Project;
import com.freelancer.model.User;
import com.freelancer.repository.PaymentRepository;
import com.freelancer.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ProjectRepository projectRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          ProjectRepository projectRepository) {
        this.paymentRepository = paymentRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getClientTransactions(long clientId) {
        return paymentRepository.findByClient_IdOrderByPaymentDateDesc(clientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FreelancerEarningsResponse getFreelancerEarnings(long freelancerId) {
        List<PaymentTransactionResponse> payments = paymentRepository.findByFreelancer_IdOrderByPaymentDateDesc(freelancerId)
                .stream()
                .map(this::toResponse)
                .toList();
        BigDecimal totalEarnings = payments.stream()
                .filter(payment -> !"PENDING".equals(payment.status())
                        && !"REFUND_SETTLED".equals(payment.status())
                        && !"REFUNDED".equals(payment.status()))
                .map(payment -> (payment.amount() == null ? BigDecimal.ZERO : payment.amount()).subtract(
                        payment.refundedAmount() == null ? BigDecimal.ZERO : payment.refundedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FreelancerEarningsResponse(payments, totalEarnings);
    }

    @Transactional
    public Payment createPendingInvoice(Project project, User freelancer, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("A positive project payment amount is required before acceptance");
        }
        if (project.getClient() == null || freelancer == null) {
            throw new IllegalArgumentException("An invoice requires a project owner and freelancer");
        }

        Payment payment = paymentRepository.findByProject_Id(project.getId()).orElseGet(Payment::new);
        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new IllegalArgumentException("This project already has payment history");
        }
        payment.setProject(project);
        payment.setClient(project.getClient());
        payment.setFreelancer(freelancer);
        payment.setAmount(amount);
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now(ZoneOffset.UTC));
        }
        payment.setStatus(Payment.Status.PENDING);
        payment.setTransactionId(null);
        return paymentRepository.save(payment);
    }

    @Transactional
    public void removePendingInvoice(long projectId, long freelancerId) {
        paymentRepository.findByProject_Id(projectId)
                .filter(payment -> payment.getStatus() == Payment.Status.PENDING)
                .filter(payment -> payment.getFreelancer().getId().equals(freelancerId))
                .ifPresent(paymentRepository::delete);
    }

    @Transactional
    public PaymentTransactionResponse pay(long clientId, PayProjectRequest request) {
        if (request.getProjectId() == null) {
            throw new IllegalArgumentException("Project id is required");
        }
        long projectId = request.getProjectId();
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        if (!project.getClient().getId().equals(clientId)) {
            throw new IllegalArgumentException("You can only pay for your own projects");
        }
        Payment payment = paymentRepository.findByProject_Id(project.getId())
                .orElseThrow(() -> new IllegalArgumentException("No invoice exists for this project"));
        if (payment.getStatus() != Payment.Status.PENDING
                || project.getPaymentStatus() == Project.PaymentStatus.PAID) {
            throw new IllegalArgumentException("This project has already been paid");
        }
        if (!payment.getClient().getId().equals(clientId)
                || !payment.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("The invoice does not belong to this client and project");
        }
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("The invoice must have a valid payment amount");
        }
        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("A payment method is required");
        }

        payment.setPaymentDate(LocalDateTime.now(ZoneOffset.UTC));
        payment.setStatus(Payment.Status.PAID);
        payment.setTransactionId("TXN-" + ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000));
        payment.setPaymentMethod(request.getPaymentMethod());
        project.setPaymentStatus(Project.PaymentStatus.PAID);
        projectRepository.save(project);

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentTransactionResponse refund(long clientId, Long projectId, String transactionId) {
        return refund(clientId, projectId, transactionId, null);
    }

    @Transactional
    public PaymentTransactionResponse refund(long clientId, Long projectId, String transactionId,
                                             BigDecimal requestedAmount) {
        boolean hasProjectId = projectId != null;
        boolean hasTransactionId = transactionId != null && !transactionId.isBlank();
        if (hasProjectId == hasTransactionId) {
            throw new IllegalArgumentException("Provide exactly one project id or transaction id");
        }

        Payment payment = (hasProjectId
                ? paymentRepository.findByProjectIdForUpdate(projectId)
                : paymentRepository.findByTransactionIdForUpdate(transactionId.trim()))
                .orElseThrow(() -> new IllegalArgumentException("No payment exists for the provided identifier"));
        if (!payment.getClient().getId().equals(clientId)) {
            throw new IllegalArgumentException("You can only refund payments for your own projects");
        }
        if (payment.getStatus() != Payment.Status.PAID
                && payment.getStatus() != Payment.Status.REFUNDED_PARTIALLY) {
            throw new IllegalArgumentException("Only paid transactions without a pending refund can be refunded");
        }

        BigDecimal remainingAmount = remainingRefundableAmount(payment);
        BigDecimal amountToRequest = requestedAmount == null ? remainingAmount : requestedAmount;
        validateRefundAmount(amountToRequest, remainingAmount);
        payment.setRequestedRefundAmount(amountToRequest);
        payment.setStatus(Payment.Status.REFUND_REQUESTED);
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentTransactionResponse submitRefund(long freelancerId, Long projectId, BigDecimal amount) {
        Payment payment = paymentRepository.findByProjectIdForUpdate(projectId)
                .orElseThrow(() -> new IllegalArgumentException("No payment exists for this project"));
        if (!payment.getFreelancer().getId().equals(freelancerId)) {
            throw new IllegalArgumentException("You can only process refunds for your own projects");
        }
        if (payment.getStatus() != Payment.Status.REFUND_REQUESTED) {
            throw new IllegalArgumentException("This project has no pending refund request");
        }

        BigDecimal remainingAmount = remainingRefundableAmount(payment);
        validateRefundAmount(amount, remainingAmount);
        BigDecimal totalRefunded = refundedAmount(payment).add(amount);
        payment.setRefundedAmount(totalRefunded);
        payment.setRequestedRefundAmount(null);
        payment.setRefundDate(LocalDateTime.now(ZoneOffset.UTC));
        payment.setStatus(totalRefunded.compareTo(payment.getAmount()) == 0
                ? Payment.Status.REFUND_SETTLED
                : Payment.Status.REFUNDED_PARTIALLY);
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentTransactionResponse rejectRefund(long freelancerId, Long projectId) {
        Payment payment = paymentRepository.findByProjectIdForUpdate(projectId)
                .orElseThrow(() -> new IllegalArgumentException("No payment exists for this project"));
        if (!payment.getFreelancer().getId().equals(freelancerId)) {
            throw new IllegalArgumentException("You can only process refunds for your own projects");
        }
        if (payment.getStatus() != Payment.Status.REFUND_REQUESTED) {
            throw new IllegalArgumentException("This project has no pending refund request");
        }

        payment.setRequestedRefundAmount(null);
        payment.setStatus(refundedAmount(payment).signum() > 0
                ? Payment.Status.REFUNDED_PARTIALLY
                : Payment.Status.PAID);
        return toResponse(paymentRepository.save(payment));
    }

    private void validateRefundAmount(BigDecimal amount, BigDecimal remainingAmount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Refund amount cannot have more than two decimal places");
        }
        if (amount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed the remaining project payment");
        }
    }

    private BigDecimal refundedAmount(Payment payment) {
        return payment.getRefundedAmount() == null ? BigDecimal.ZERO : payment.getRefundedAmount();
    }

    private BigDecimal remainingRefundableAmount(Payment payment) {
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("The payment must have a valid amount before it can be refunded");
        }
        BigDecimal remainingAmount = payment.getAmount().subtract(refundedAmount(payment));
        if (remainingAmount.signum() <= 0) {
            throw new IllegalArgumentException("No refundable balance remains for this project");
        }
        return remainingAmount;
    }

    private PaymentTransactionResponse toResponse(Payment payment) {
        Project project = payment.getProject();
        User client = payment.getClient();
        User freelancer = payment.getFreelancer();
        return new PaymentTransactionResponse(
                payment.getId(), project.getId(), project.getTitle(),
                client.getFullName(), freelancer.getFullName(), payment.getAmount(),
                payment.getPaymentDate(), payment.getStatus().name(), payment.getTransactionId(),
                payment.getPaymentMethod() == null ? null : payment.getPaymentMethod().name(),
                payment.getRefundDate(), payment.getRequestedRefundAmount(), refundedAmount(payment));
    }
}
