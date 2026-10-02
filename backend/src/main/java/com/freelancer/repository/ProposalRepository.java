package com.freelancer.repository;

import com.freelancer.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByProjectId(Long projectId);
    List<Proposal> findByFreelancerId(Long freelancerId);
    Optional<Proposal> findByProjectIdAndFreelancerId(Long projectId, Long freelancerId);
    boolean existsByProjectIdAndFreelancerId(Long projectId, Long freelancerId);
    List<Proposal> findByFreelancerIdAndStatus(Long freelancerId, Proposal.Status status);
    void deleteByProjectId(Long projectId);
}
