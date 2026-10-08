package com.freelancer.service;

import com.freelancer.dto.ProjectRequest;
import com.freelancer.model.HiringRequest;
import com.freelancer.model.Proposal;
import com.freelancer.model.Project;
import com.freelancer.model.User;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.HiringRequestRepository;
import com.freelancer.repository.PaymentRepository;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ProjectService {

    @Autowired private ProjectRepository projectRepository;
    @Autowired private HiringRequestRepository hiringRequestRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private UserRepository userRepository;

    public Project createProject(Long clientId, ProjectRequest request) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Project project = new Project();
        project.setClient(client);
        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setRequiredSkills(request.getRequiredSkills());
        project.setBudget(request.getBudget());
        if (request.getDeadline() != null && !request.getDeadline().isBlank()) {
            project.setDeadline(LocalDate.parse(request.getDeadline()));
        }
        project.setStatus(Project.Status.OPEN);

        return projectRepository.save(project);
    }

    public Project updateProject(Long projectId, Long clientId, ProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Unauthorized: Not your project");
        }

        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setRequiredSkills(request.getRequiredSkills());
        project.setBudget(request.getBudget());
        if (request.getDeadline() != null && !request.getDeadline().isBlank()) {
            project.setDeadline(LocalDate.parse(request.getDeadline()));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            Project.Status requestedStatus = Project.Status.valueOf(request.getStatus().toUpperCase(Locale.ROOT));
            validateCompletionTransition(project, requestedStatus);
            project.setStatus(requestedStatus);
        }

        return projectRepository.save(project);
    }

    public Project updateStatus(Long projectId, Long clientId, String status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Unauthorized: Not your project");
        }

        Project.Status requestedStatus = Project.Status.valueOf(status.toUpperCase(Locale.ROOT));
        if (requestedStatus == Project.Status.SUBMITTED) {
            throw new IllegalArgumentException("Only the hired freelancer can submit project work");
        }
        project.setStatus(requestedStatus);
        return projectRepository.save(project);
    }

    @Transactional
    public Project submitWork(Long projectId, Long freelancerId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        boolean acceptedProposal = proposalRepository.findByProjectIdAndFreelancerId(projectId, freelancerId)
                .map(proposal -> proposal.getStatus() == Proposal.Status.ACCEPTED)
                .orElse(false);
        boolean acceptedHiring = hiringRequestRepository.existsByProjectIdAndFreelancerIdAndStatus(
                projectId, freelancerId, HiringRequest.Status.ACCEPTED);
        if (!acceptedProposal && !acceptedHiring) {
            throw new RuntimeException("Only the hired freelancer can submit work");
        }
        if (project.getStatus() != Project.Status.IN_PROGRESS) {
            throw new RuntimeException("Work can only be submitted for an in-progress project");
        }

        project.setStatus(Project.Status.SUBMITTED);
        return projectRepository.save(project);
    }

    private void validateCompletionTransition(Project project, Project.Status requestedStatus) {
        if (requestedStatus == Project.Status.IN_PROGRESS
                && project.getStatus() != Project.Status.IN_PROGRESS
                && proposalRepository.findByProjectId(project.getId()).isEmpty()) {
            throw new IllegalArgumentException("A project can only start after a proposal has been submitted");
        }
        if (requestedStatus == Project.Status.SUBMITTED && project.getStatus() != Project.Status.SUBMITTED) {
            throw new IllegalArgumentException("Only the hired freelancer can submit project work");
        }
        if (requestedStatus == Project.Status.COMPLETED
                && project.getStatus() != Project.Status.SUBMITTED
                && project.getStatus() != Project.Status.COMPLETED) {
            throw new IllegalArgumentException("A project can only be completed after the freelancer submits work");
        }
    }

    public Project getById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public List<Project> getOpenProjects() {
        return projectRepository.findByStatus(Project.Status.OPEN);
    }

    public List<Project> getProjectsByClient(Long clientId) {
        return projectRepository.findByClientId(clientId);
    }

    public List<Project> searchByKeyword(String keyword) {
        return projectRepository.findOpenProjectsByKeyword(keyword);
    }

    public List<Project> searchBySkill(String skill) {
        return projectRepository.findOpenProjectsBySkill(skill);
    }

    @Transactional
    public void deleteProject(Long projectId, Long clientId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Unauthorized: Not your project");
        }
        paymentRepository.deleteByProjectId(projectId);
        proposalRepository.deleteByProjectId(projectId);
        hiringRequestRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }
}
