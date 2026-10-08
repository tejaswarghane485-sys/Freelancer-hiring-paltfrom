package com.freelancer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_project_id", columnNames = "project_id"),
        @UniqueConstraint(name = "uk_payments_transaction_id", columnNames = "transaction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50,
            columnDefinition = "varchar(50) not null default 'PENDING'")
    private Status status = Status.PENDING;

    @Column(name = "transaction_id", length = 36)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private Method paymentMethod;

    public enum Status {
        PAID, PENDING, REFUNDED
    }

    public enum Method {
        UPI, NET_BANKING, CARD
    }
}
