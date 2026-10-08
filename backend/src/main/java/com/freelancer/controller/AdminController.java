package com.freelancer.controller;

import com.freelancer.dto.ProjectResponse;
import com.freelancer.model.User;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.UserRepository;
import com.freelancer.service.AdminService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProposalRepository proposalRepository;
    private final AdminService adminService;

    public AdminController(UserRepository userRepository,
                           ProjectRepository projectRepository,
                           ProposalRepository proposalRepository,
                           AdminService adminService) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.proposalRepository = proposalRepository;
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public Stats getStats() {
        return new Stats(
                userRepository.count(),
                userRepository.countByRole(User.Role.CLIENT),
                userRepository.countByRole(User.Role.FREELANCER),
                projectRepository.count(),
                proposalRepository.count()
        );
    }

    @GetMapping("/users")
    public List<UserSummary> getUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserSummary(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getCreatedAt()))
                .toList();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/projects")
    public List<ProjectResponse> getProjects() {
        return adminService.getProjects();
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        adminService.deleteProject(id);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleAdminStatusException(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErrorResponse(exception.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleAdminDeleteConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Cannot delete this record because related records still reference it"));
    }

    public record Stats(long totalUsers, long totalClients, long totalFreelancers,
                        long totalProjects, long totalProposals) {
    }

    public record UserSummary(Long id, String name, String email, User.Role role,
                              LocalDateTime createdAt) {
    }

    public record ErrorResponse(String message) {
    }
}
