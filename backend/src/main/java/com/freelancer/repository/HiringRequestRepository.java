package com.freelancer.repository;

import com.freelancer.model.HiringRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HiringRequestRepository extends JpaRepository<HiringRequest, Long> {
    List<HiringRequest> findByFreelancerId(Long freelancerId);
    List<HiringRequest> findByClientId(Long clientId);
    List<HiringRequest> findByProjectId(Long projectId);
    List<HiringRequest> findByFreelancerIdAndStatus(Long freelancerId, HiringRequest.Status status);
    List<HiringRequest> findByClientIdAndStatus(Long clientId, HiringRequest.Status status);
    boolean existsByProjectIdAndFreelancerId(Long projectId, Long freelancerId);
    void deleteByProjectId(Long projectId);
}
