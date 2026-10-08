package com.freelancer.repository;

import com.freelancer.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByProjectId(Long projectId);
    List<Proposal> findByStatus(Proposal.Status status);
    List<Proposal> findByProject_Client_IdAndStatus(Long clientId, Proposal.Status status);
    List<Proposal> findByFreelancerId(Long freelancerId);
    Optional<Proposal> findByProjectIdAndFreelancerId(Long projectId, Long freelancerId);
    boolean existsByProjectIdAndFreelancerId(Long projectId, Long freelancerId);
    boolean existsByProjectId(Long projectId);
    List<Proposal> findByFreelancerIdAndStatus(Long freelancerId, Proposal.Status status);

    @Modifying(flushAutomatically = true)
    @Query("delete from Proposal proposal where proposal.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}
