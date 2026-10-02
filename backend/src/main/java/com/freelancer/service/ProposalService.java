package com.freelancer.service;

import com.freelancer.dto.ProposalRequest;
import com.freelancer.model.Proposal;
import com.freelancer.model.Project;
import com.freelancer.model.User;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProposalService {

    @Autowired private ProposalRepository proposalRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;

    public Proposal submitProposal(Long freelancerId, ProposalRequest request) {
        if (proposalRepository.existsByProjectIdAndFreelancerId(request.getProjectId(), freelancerId)) {
            throw new RuntimeException("You have already submitted a proposal for this project");
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (project.getStatus() != Project.Status.OPEN) {
            throw new RuntimeException("Project is not open for proposals");
        }

        User freelancer = userRepository.findById(freelancerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Proposal proposal = new Proposal();
        proposal.setProject(project);
        proposal.setFreelancer(freelancer);
        proposal.setCoverLetter(request.getCoverLetter());
        proposal.setProposedBudget(request.getProposedBudget());
        proposal.setEstimatedDays(request.getEstimatedDays());
        proposal.setStatus(Proposal.Status.PENDING);

        return proposalRepository.save(proposal);
    }

    public List<Proposal> getProposalsForProject(Long projectId) {
        return proposalRepository.findByProjectId(projectId);
    }

    public List<Proposal> getProposalsByFreelancer(Long freelancerId) {
        return proposalRepository.findByFreelancerId(freelancerId);
    }

    public Proposal updateProposalStatus(Long proposalId, Long clientId, String status) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (!proposal.getProject().getClient().getId().equals(clientId)) {
            throw new RuntimeException("Unauthorized");
        }

        proposal.setStatus(Proposal.Status.valueOf(status.toUpperCase()));
        return proposalRepository.save(proposal);
    }

    public void deleteProposal(Long proposalId, Long freelancerId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));
        if (!proposal.getFreelancer().getId().equals(freelancerId)) {
            throw new RuntimeException("Unauthorized");
        }
        proposalRepository.delete(proposal);
    }
}
