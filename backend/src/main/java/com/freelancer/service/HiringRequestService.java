package com.freelancer.service;

import com.freelancer.dto.HiringRequestDto;
import com.freelancer.model.HiringRequest;
import com.freelancer.model.Project;
import com.freelancer.model.User;
import com.freelancer.repository.HiringRequestRepository;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HiringRequestService {

    @Autowired private HiringRequestRepository hiringRequestRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;

    public HiringRequest sendHiringRequest(Long clientId, HiringRequestDto dto) {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Unauthorized: Not your project");
        }

        // Guard: prevent duplicate hiring requests for same project+freelancer
        if (hiringRequestRepository.existsByProjectIdAndFreelancerId(dto.getProjectId(), dto.getFreelancerId())) {
            throw new RuntimeException("You have already sent a hiring request to this freelancer for this project");
        }

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        User freelancer = userRepository.findById(dto.getFreelancerId())
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        if (freelancer.getRole() != com.freelancer.model.User.Role.FREELANCER) {
            throw new RuntimeException("Target user is not a freelancer");
        }

        HiringRequest hiringRequest = new HiringRequest();
        hiringRequest.setProject(project);
        hiringRequest.setClient(client);
        hiringRequest.setFreelancer(freelancer);
        hiringRequest.setMessage(dto.getMessage());
        hiringRequest.setStatus(HiringRequest.Status.PENDING);

        return hiringRequestRepository.save(hiringRequest);
    }

    public HiringRequest respondToRequest(Long requestId, Long freelancerId, String status) {
        HiringRequest request = hiringRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Hiring request not found"));

        if (!request.getFreelancer().getId().equals(freelancerId)) {
            throw new RuntimeException("Unauthorized");
        }

        request.setStatus(HiringRequest.Status.valueOf(status.toUpperCase()));

        // If accepted, update project status to IN_PROGRESS
        if (request.getStatus() == HiringRequest.Status.ACCEPTED) {
            Project project = request.getProject();
            project.setStatus(Project.Status.IN_PROGRESS);
            projectRepository.save(project);
        }

        return hiringRequestRepository.save(request);
    }

    public List<HiringRequest> getRequestsForFreelancer(Long freelancerId) {
        return hiringRequestRepository.findByFreelancerId(freelancerId);
    }

    public List<HiringRequest> getRequestsByClient(Long clientId) {
        return hiringRequestRepository.findByClientId(clientId);
    }

    public List<HiringRequest> getRequestsForProject(Long projectId) {
        return hiringRequestRepository.findByProjectId(projectId);
    }
}
