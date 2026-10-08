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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProposalService {

    @Autowired private ProposalRepository proposalRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PaymentService paymentService;

    @Transactional
    public Proposal submitProposal(Long freelancerId, ProposalRequest request) {
        if (proposalRepository.existsByProjectIdAndFreelancerId(request.getProjectId(), freelancerId)) {
            throw new RuntimeException("You have already submitted a proposal for this project");
        }

        Project project = projectRepository.findByIdForUpdate(request.getProjectId())
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

        Proposal savedProposal = proposalRepository.save(proposal);
        project.setStatus(Project.Status.IN_PROGRESS);
        projectRepository.save(project);
        return savedProposal;
    }

    public List<Proposal> getProposalsForProject(Long projectId) {
        return proposalRepository.findByProjectId(projectId);
    }

    public List<Proposal> getProposalsByFreelancer(Long freelancerId) {
        return proposalRepository.findByFreelancerId(freelancerId);
    }

    @Transactional
    public Proposal updateProposalStatus(Long proposalId, Long clientId, String status) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (!proposal.getProject().getClient().getId().equals(clientId)) {
            throw new RuntimeException("Unauthorized");
        }

        Proposal.Status newStatus = Proposal.Status.valueOf(status.toUpperCase());
        Project project = projectRepository.findByIdForUpdate(proposal.getProject().getId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        Proposal.Status previousStatus = proposal.getStatus();
        if (newStatus == Proposal.Status.ACCEPTED) {
            if (project.getPaymentStatus() == Project.PaymentStatus.PAID) {
                throw new RuntimeException("This project has already been paid");
            }
            proposalRepository.findByProjectId(project.getId()).stream()
                    .filter(other -> !other.getId().equals(proposalId)
                            && other.getStatus() == Proposal.Status.ACCEPTED)
                    .forEach(other -> {
                        other.setStatus(Proposal.Status.REJECTED);
                        proposalRepository.save(other);
                    });
        } else if (previousStatus == Proposal.Status.ACCEPTED) {
            if (project.getPaymentStatus() == Project.PaymentStatus.PAID) {
                throw new RuntimeException("A paid proposal cannot be changed");
            }
        }
        proposal.setProject(project);
        proposal.setStatus(newStatus);
        Proposal savedProposal;
        if (newStatus == Proposal.Status.ACCEPTED) {
            java.math.BigDecimal amount = proposal.getProposedBudget() != null
                    ? proposal.getProposedBudget()
                    : project.getBudget();
            if (amount == null || amount.signum() <= 0) {
                throw new RuntimeException("Set a positive proposal or project amount before accepting");
            }
            savedProposal = proposalRepository.save(proposal);
            paymentService.createPendingInvoice(project, proposal.getFreelancer(), amount);
            project.setStatus(Project.Status.IN_PROGRESS);
            projectRepository.save(project);
        } else if (previousStatus == Proposal.Status.ACCEPTED) {
            paymentService.removePendingInvoice(project.getId(), proposal.getFreelancer().getId());
            savedProposal = proposalRepository.save(proposal);
        } else {
            savedProposal = proposalRepository.save(proposal);
        }
        return savedProposal;
    }

    @Transactional
    public void deleteProposal(Long proposalId, Long freelancerId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));
        if (!proposal.getFreelancer().getId().equals(freelancerId)) {
            throw new RuntimeException("Unauthorized");
        }
        Project project = projectRepository.findByIdForUpdate(proposal.getProject().getId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (proposal.getStatus() == Proposal.Status.ACCEPTED) {
            if (project.getPaymentStatus() == Project.PaymentStatus.PAID) {
                throw new RuntimeException("A paid proposal cannot be deleted");
            }
            paymentService.removePendingInvoice(project.getId(), proposal.getFreelancer().getId());
        }
        proposalRepository.delete(proposal);
    }
}
