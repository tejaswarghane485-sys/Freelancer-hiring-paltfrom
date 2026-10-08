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
import java.util.Objects;
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
                .filter(payment -> "PAID".equals(payment.status()))
                .map(PaymentTransactionResponse::amount)
                .filter(Objects::nonNull)
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
        if (payment.getStatus() == Payment.Status.PAID || payment.getStatus() == Payment.Status.REFUNDED) {
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
        if (payment.getStatus() == Payment.Status.PAID
                || payment.getStatus() == Payment.Status.REFUNDED
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
        if (payment.getStatus() != Payment.Status.PAID) {
            throw new IllegalArgumentException("Only paid transactions can be refunded");
        }

        payment.setStatus(Payment.Status.REFUNDED);
        payment.setRefundDate(LocalDateTime.now(ZoneOffset.UTC));
        return toResponse(paymentRepository.save(payment));
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
                payment.getRefundDate());
    }
}
