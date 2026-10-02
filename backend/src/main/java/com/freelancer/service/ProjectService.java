package com.freelancer.service;

import com.freelancer.dto.ProjectRequest;
import com.freelancer.model.Project;
import com.freelancer.model.User;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.HiringRequestRepository;
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
            project.setStatus(Project.Status.valueOf(request.getStatus().toUpperCase(Locale.ROOT)));
        }

        return projectRepository.save(project);
    }

    public Project updateStatus(Long projectId, Long clientId, String status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Unauthorized: Not your project");
        }

        project.setStatus(Project.Status.valueOf(status.toUpperCase()));
        return projectRepository.save(project);
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
        return projectRepository.findByKeyword(keyword);
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
        hiringRequestRepository.deleteByProjectId(projectId);
        proposalRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
    }
}
