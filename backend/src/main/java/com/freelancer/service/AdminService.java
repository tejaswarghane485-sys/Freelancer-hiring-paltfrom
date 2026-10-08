package com.freelancer.service;

import com.freelancer.dto.ProjectResponse;
import com.freelancer.model.HiringRequest;
import com.freelancer.model.Payment;
import com.freelancer.model.Proposal;
import com.freelancer.model.Project;
import com.freelancer.model.User;
import com.freelancer.repository.HiringRequestRepository;
import com.freelancer.repository.PaymentRepository;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProposalRepository proposalRepository;
    private final HiringRequestRepository hiringRequestRepository;
    private final PaymentRepository paymentRepository;

    public AdminService(UserRepository userRepository,
                       ProjectRepository projectRepository,
                       ProposalRepository proposalRepository,
                       HiringRequestRepository hiringRequestRepository,
                       PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.proposalRepository = proposalRepository;
        this.hiringRequestRepository = hiringRequestRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        Map<Long, String> freelancerNamesByProject = new HashMap<>();
        Map<Long, BigDecimal> freelancerPricesByProject = new HashMap<>();
        proposalRepository.findByStatus(Proposal.Status.ACCEPTED).forEach(proposal ->
                {
                    Long projectId = proposal.getProject().getId();
                    freelancerNamesByProject.putIfAbsent(projectId, proposal.getFreelancer().getFullName());
                    freelancerPricesByProject.putIfAbsent(
                            projectId,
                            proposal.getProposedBudget() != null
                                    ? proposal.getProposedBudget()
                                    : proposal.getProject().getBudget());
                });
        hiringRequestRepository.findByStatus(HiringRequest.Status.ACCEPTED).forEach(request ->
                {
                    Long projectId = request.getProject().getId();
                    freelancerNamesByProject.putIfAbsent(projectId, request.getFreelancer().getFullName());
                    freelancerPricesByProject.putIfAbsent(projectId, request.getProject().getBudget());
                });

        List<Project> projects = projectRepository.findAll();
        Map<Long, BigDecimal> paymentAmountsByProject = projects.isEmpty()
                ? Map.of()
                : paymentRepository.findByProjectIds(
                                projects.stream().map(Project::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(
                                payment -> payment.getProject().getId(),
                                Payment::getAmount));

        return projects.stream()
                .map(project -> {
                    BigDecimal paidOrAgreedPrice = paymentAmountsByProject.getOrDefault(
                            project.getId(), freelancerPricesByProject.get(project.getId()));
                    return new ProjectResponse(
                            project.getId(),
                            project.getTitle(),
                            project.getClient().getFullName(),
                            freelancerNamesByProject.getOrDefault(project.getId(), "Not Assigned"),
                            project.getBudget(),
                            paidOrAgreedPrice,
                            project.getStatus());
                })
                .toList();
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot be deleted here");
        }
        userRepository.delete(user);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        paymentRepository.deleteByProjectId(projectId);
        proposalRepository.deleteByProjectId(projectId);
        hiringRequestRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }
}
