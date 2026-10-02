package com.freelancer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "freelancer_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Eager-load project and freelancer so their names appear in responses
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnoreProperties({"client", "createdAt", "updatedAt", "description"})
    private Project project;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "freelancer_id", nullable = false)
    @JsonIgnoreProperties({"password", "createdAt", "updatedAt"})
    private User freelancer;

    @NotBlank
    @Column(name = "cover_letter", nullable = false, columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "proposed_budget", precision = 12, scale = 2)
    private BigDecimal proposedBudget;

    @Column(name = "estimated_days")
    private Integer estimatedDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }
}
