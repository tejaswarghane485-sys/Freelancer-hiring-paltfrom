package com.freelancer.repository;

import com.freelancer.model.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByClient_IdOrderByPaymentDateDesc(Long clientId);
    List<Payment> findByFreelancer_IdOrderByPaymentDateDesc(Long freelancerId);
    Optional<Payment> findByProject_Id(Long projectId);

    @Query("select payment from Payment payment where payment.project.id in :projectIds")
    List<Payment> findByProjectIds(@Param("projectIds") List<Long> projectIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from Payment payment where payment.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.project.id = :projectId")
    Optional<Payment> findByProjectIdForUpdate(@Param("projectId") Long projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.transactionId = :transactionId")
    Optional<Payment> findByTransactionIdForUpdate(@Param("transactionId") String transactionId);
}
