package com.freelancer.service;

import com.freelancer.dto.PayProjectRequest;
import com.freelancer.dto.FreelancerEarningsResponse;
import com.freelancer.dto.PaymentTransactionResponse;
import com.freelancer.model.Payment;
import com.freelancer.model.Project;
import com.freelancer.model.User;
import com.freelancer.repository.PaymentRepository;
import com.freelancer.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void payRecordsAcceptedProposalAmountAndMarksProjectPaid() {
        User client = new User();
        client.setId(5L);
        client.setFullName("Client Name");
        User freelancer = new User();
        freelancer.setId(9L);
        freelancer.setFullName("Freelancer Name");
        Project project = new Project();
        project.setId(20L);
        project.setClient(client);
        project.setTitle("Build a website");
        Payment payment = new Payment();
        payment.setId(40L);
        payment.setProject(project);
        payment.setClient(client);
        payment.setFreelancer(freelancer);
        payment.setAmount(new BigDecimal("450.00"));
        payment.setStatus(Payment.Status.PENDING);

        when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
        when(paymentRepository.findByProject_Id(20L)).thenReturn(Optional.of(payment));
        when(projectRepository.save(project)).thenReturn(project);
        when(paymentRepository.save(payment)).thenReturn(payment);
        PayProjectRequest request = new PayProjectRequest();
        request.setProjectId(20L);
        request.setPaymentMethod(Payment.Method.CARD);
        PaymentTransactionResponse result = paymentService.pay(5L, request);

        assertEquals(new BigDecimal("450.00"), result.amount());
        assertEquals("PAID", result.status());
        assertEquals(Project.PaymentStatus.PAID, project.getPaymentStatus());
        assertEquals("Freelancer Name", result.freelancerName());
        assertEquals(12, result.transactionId().length());
        assertTrue(result.transactionId().matches("TXN-\\d{8}"));
        assertEquals(Payment.Method.CARD, payment.getPaymentMethod());
        assertEquals("CARD", result.paymentMethod());
        assertTrue(result.paymentDate() != null);
    }

    @Test
    void payRejectsAProjectOwnedByAnotherClient() {
        User owner = new User();
        owner.setId(5L);
        Project project = new Project();
        project.setId(20L);
        project.setClient(owner);
        when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));

        PayProjectRequest request = new PayProjectRequest();
        request.setProjectId(20L);

        assertThrows(IllegalArgumentException.class, () -> paymentService.pay(8L, request));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void payRejectsProjectsWithoutPersistedInvoices() {
        User owner = new User();
        owner.setId(5L);
        Project project = new Project();
        project.setId(20L);
        project.setClient(owner);
        when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
        when(paymentRepository.findByProject_Id(20L)).thenReturn(Optional.empty());

        PayProjectRequest request = new PayProjectRequest();
        request.setProjectId(20L);

        assertThrows(IllegalArgumentException.class, () -> paymentService.pay(5L, request));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundRequestDoesNotDeductFundsOrChangeItsTransactionId() {
        User client = new User();
        client.setId(5L);
        client.setFullName("Client Name");
        User freelancer = new User();
        freelancer.setFullName("Freelancer Name");
        Project project = new Project();
        project.setId(20L);
        project.setTitle("Build a website");
        Payment payment = new Payment();
        payment.setId(40L);
        payment.setProject(project);
        payment.setClient(client);
        payment.setFreelancer(freelancer);
        payment.setAmount(new BigDecimal("450.00"));
        payment.setStatus(Payment.Status.PAID);
        payment.setTransactionId("TXN-12345678");
        when(paymentRepository.findByProjectIdForUpdate(20L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentTransactionResponse result = paymentService.refund(
                5L, 20L, null, new BigDecimal("150.00"));

        assertEquals("REFUND_REQUESTED", result.status());
        assertEquals("TXN-12345678", result.transactionId());
        assertEquals(new BigDecimal("150.00"), result.requestedRefundAmount());
        assertEquals(BigDecimal.ZERO, result.refundedAmount());
        assertEquals(null, result.refundDate());
        verify(paymentRepository).save(payment);
    }

    @Test
    void refundRejectsPaymentsBelongingToAnotherClient() {
        User client = new User();
        client.setId(5L);
        Payment payment = new Payment();
        payment.setClient(client);
        payment.setStatus(Payment.Status.PAID);
        when(paymentRepository.findByProjectIdForUpdate(20L)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> paymentService.refund(8L, 20L, null));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundCanFindTransactionByTransactionId() {
        User client = new User();
        client.setId(5L);
        User freelancer = new User();
        Project project = new Project();
        project.setId(20L);
        Payment payment = new Payment();
        payment.setProject(project);
        payment.setClient(client);
        payment.setFreelancer(freelancer);
        payment.setAmount(new BigDecimal("450.00"));
        payment.setStatus(Payment.Status.PAID);
        payment.setTransactionId("TXN-12345678");
        when(paymentRepository.findByTransactionIdForUpdate("TXN-12345678"))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentTransactionResponse result = paymentService.refund(5L, null, " TXN-12345678 ");

        assertEquals("REFUND_REQUESTED", result.status());
        verify(paymentRepository).findByTransactionIdForUpdate("TXN-12345678");
    }

    @Test
    void freelancerCanSubmitPartialRefundForTheRequestedAmount() {
        User client = new User();
        client.setId(5L);
        client.setFullName("Client Name");
        User freelancer = new User();
        freelancer.setId(9L);
        freelancer.setFullName("Freelancer Name");
        Project project = new Project();
        project.setId(20L);
        project.setTitle("Build a website");
        Payment payment = new Payment();
        payment.setProject(project);
        payment.setClient(client);
        payment.setFreelancer(freelancer);
        payment.setAmount(new BigDecimal("450.00"));
        payment.setRequestedRefundAmount(new BigDecimal("200.00"));
        payment.setStatus(Payment.Status.REFUND_REQUESTED);
        when(paymentRepository.findByProjectIdForUpdate(20L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentTransactionResponse result = paymentService.submitRefund(9L, 20L, new BigDecimal("175.00"));

        assertEquals("REFUNDED_PARTIALLY", result.status());
        assertEquals(new BigDecimal("175.00"), result.refundedAmount());
        assertEquals(null, result.requestedRefundAmount());
        assertTrue(result.refundDate() != null);
    }

    @Test
    void finalPartialRefundSettlesPayment() {
        User client = new User();
        client.setId(5L);
        User freelancer = new User();
        freelancer.setId(9L);
        Project project = new Project();
        project.setId(20L);
        project.setTitle("Build a website");
        Payment payment = new Payment();
        payment.setProject(project);
        payment.setClient(client);
        payment.setFreelancer(freelancer);
        payment.setAmount(new BigDecimal("450.00"));
        payment.setRefundedAmount(new BigDecimal("175.00"));
        payment.setStatus(Payment.Status.REFUND_REQUESTED);
        when(paymentRepository.findByProjectIdForUpdate(20L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentTransactionResponse result = paymentService.submitRefund(9L, 20L, new BigDecimal("275.00"));

        assertEquals("REFUND_SETTLED", result.status());
        assertEquals(new BigDecimal("450.00"), result.refundedAmount());
    }

    @Test
    void refundApprovalRejectsAmountsAboveRemainingBalance() {
        User client = new User();
        client.setId(5L);
        User freelancer = new User();
        freelancer.setId(9L);
        Project project = new Project();
        project.setId(20L);
        Payment payment = new Payment();
        payment.setProject(project);
        payment.setClient(client);
        payment.setFreelancer(freelancer);
        payment.setAmount(new BigDecimal("450.00"));
        payment.setRefundedAmount(new BigDecimal("175.00"));
        payment.setStatus(Payment.Status.REFUND_REQUESTED);
        when(paymentRepository.findByProjectIdForUpdate(20L)).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.submitRefund(9L, 20L, new BigDecimal("275.01")));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void freelancerCanRejectRefundWithoutChangingPaidAmount() {
        User client = new User();
        client.setId(5L);
        User freelancer = new User();
        freelancer.setId(9L);
        Project project = new Project();
        project.setId(20L);
        project.setTitle("Build a website");
        Payment payment = new Payment();
        payment.setProject(project);
        payment.setClient(client);
        payment.setFreelancer(freelancer);
        payment.setAmount(new BigDecimal("450.00"));
        payment.setRequestedRefundAmount(new BigDecimal("100.00"));
        payment.setStatus(Payment.Status.REFUND_REQUESTED);
        when(paymentRepository.findByProjectIdForUpdate(20L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentTransactionResponse result = paymentService.rejectRefund(9L, 20L);

        assertEquals("PAID", result.status());
        assertEquals(new BigDecimal("450.00"), result.amount());
        assertEquals(BigDecimal.ZERO, result.refundedAmount());
        assertEquals(null, result.requestedRefundAmount());
    }

    @Test
    void refundRequiresExactlyOnePaymentIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> paymentService.refund(5L, null, null));
        assertThrows(IllegalArgumentException.class, () -> paymentService.refund(5L, 20L, "TXN-12345678"));
    }

    @Test
    void clientTransactionsQueryUsesTheAuthenticatedClientId() {
        Payment payment = new Payment();
        User client = new User();
        client.setId(5L);
        client.setFullName("Client Name");
        User freelancer = new User();
        freelancer.setFullName("Freelancer Name");
        Project project = new Project();
        project.setId(20L);
        project.setTitle("Build a website");
        payment.setProject(project);
        payment.setClient(client);
        payment.setFreelancer(freelancer);
        payment.setAmount(new BigDecimal("450.00"));
        payment.setStatus(Payment.Status.PENDING);
        when(paymentRepository.findByClient_IdOrderByPaymentDateDesc(5L)).thenReturn(List.of(payment));

        List<PaymentTransactionResponse> result = paymentService.getClientTransactions(5L);

        assertEquals(1, result.size());
        assertEquals("Build a website", result.get(0).projectTitle());
        assertEquals("Client Name", result.get(0).clientName());
        assertEquals("Freelancer Name", result.get(0).freelancerName());
        assertEquals("PENDING", result.get(0).status());
        verify(paymentRepository).findByClient_IdOrderByPaymentDateDesc(5L);
    }

    @Test
    void pendingInvoiceUsesProjectOwnerAsClientAndRequestedFreelancer() {
        User owner = new User();
        owner.setId(5L);
        User freelancer = new User();
        freelancer.setId(9L);
        Project project = new Project();
        project.setId(20L);
        project.setClient(owner);
        when(paymentRepository.findByProject_Id(20L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.createPendingInvoice(
                project, freelancer, new BigDecimal("450.00"));

        assertEquals(project, result.getProject());
        assertEquals(owner, result.getClient());
        assertEquals(5L, result.getClient().getId());
        assertEquals(freelancer, result.getFreelancer());
        assertEquals(9L, result.getFreelancer().getId());
        assertEquals(Payment.Status.PENDING, result.getStatus());
    }

    @Test
    void freelancerEarningsSubtractOnlyRefundsThatWereActuallySettled() {
        User client = new User();
        client.setFullName("Client Name");
        User freelancer = new User();
        freelancer.setFullName("Freelancer Name");
        Project project = new Project();
        project.setId(20L);
        project.setTitle("Build a website");

        Payment paid = new Payment();
        paid.setId(40L);
        paid.setProject(project);
        paid.setClient(client);
        paid.setFreelancer(freelancer);
        paid.setAmount(new BigDecimal("450.00"));
        paid.setStatus(Payment.Status.PAID);
        paid.setTransactionId("transaction-40");
        Payment refunded = new Payment();
        refunded.setId(41L);
        refunded.setProject(project);
        refunded.setClient(client);
        refunded.setFreelancer(freelancer);
        refunded.setAmount(new BigDecimal("150.00"));
        refunded.setStatus(Payment.Status.REFUNDED);
        refunded.setTransactionId("transaction-41");
        Payment partial = new Payment();
        partial.setId(42L);
        partial.setProject(project);
        partial.setClient(client);
        partial.setFreelancer(freelancer);
        partial.setAmount(new BigDecimal("300.00"));
        partial.setRefundedAmount(new BigDecimal("100.00"));
        partial.setStatus(Payment.Status.REFUNDED_PARTIALLY);
        partial.setTransactionId("transaction-42");

        when(paymentRepository.findByFreelancer_IdOrderByPaymentDateDesc(9L))
                .thenReturn(List.of(paid, refunded, partial));

        FreelancerEarningsResponse result = paymentService.getFreelancerEarnings(9L);

        assertEquals(new BigDecimal("650.00"), result.totalEarnings());
        assertEquals(3, result.payments().size());
        assertEquals("transaction-40", result.payments().get(0).transactionId());
        assertEquals("REFUNDED", result.payments().get(1).status());
    }
}
