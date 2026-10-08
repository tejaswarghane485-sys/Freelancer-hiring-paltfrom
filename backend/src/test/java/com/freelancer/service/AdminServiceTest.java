package com.freelancer.service;

import com.freelancer.model.Project;
import com.freelancer.model.Proposal;
import com.freelancer.model.HiringRequest;
import com.freelancer.model.Payment;
import com.freelancer.model.User;
import com.freelancer.repository.HiringRequestRepository;
import com.freelancer.repository.PaymentRepository;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private HiringRequestRepository hiringRequestRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getProjectsIncludesAssignedFreelancerOrNotAssignedLabel() {
        User client = new User();
        client.setFullName("Client Name");
        User freelancer = new User();
        freelancer.setFullName("Freelancer Name");
        Project assignedProject = new Project();
        assignedProject.setId(7L);
        assignedProject.setTitle("Assigned project");
        assignedProject.setClient(client);
        assignedProject.setBudget(new BigDecimal("1000.00"));
        assignedProject.setStatus(Project.Status.IN_PROGRESS);
        Project openProject = new Project();
        openProject.setId(8L);
        openProject.setTitle("Open project");
        openProject.setClient(client);
        openProject.setStatus(Project.Status.OPEN);
        Proposal acceptedProposal = new Proposal();
        acceptedProposal.setProject(assignedProject);
        acceptedProposal.setFreelancer(freelancer);
        acceptedProposal.setProposedBudget(new BigDecimal("750.00"));
        HiringRequest acceptedRequest = new HiringRequest();
        acceptedRequest.setProject(assignedProject);
        acceptedRequest.setFreelancer(freelancer);
        when(proposalRepository.findByStatus(Proposal.Status.ACCEPTED)).thenReturn(
                java.util.List.of(acceptedProposal));
        when(hiringRequestRepository.findByStatus(HiringRequest.Status.ACCEPTED)).thenReturn(
                java.util.List.of(acceptedRequest));
        when(projectRepository.findAll()).thenReturn(java.util.List.of(assignedProject, openProject));

        var responses = adminService.getProjects();

        assertEquals("Freelancer Name", responses.get(0).freelancerName());
        assertEquals("Not Assigned", responses.get(1).freelancerName());
        assertEquals("Client Name", responses.get(0).clientName());
        assertEquals(new BigDecimal("1000.00"), responses.get(0).clientBudget());
        assertEquals(new BigDecimal("750.00"), responses.get(0).freelancerPrice());
        assertNull(responses.get(1).freelancerPrice());
    }

    @Test
    void getProjectsPrefersPaymentAmountOverAcceptedBid() {
        User client = new User();
        client.setFullName("Client Name");
        User freelancer = new User();
        freelancer.setFullName("Freelancer Name");
        Project project = new Project();
        project.setId(7L);
        project.setClient(client);
        project.setBudget(new BigDecimal("1000.00"));
        Proposal acceptedProposal = new Proposal();
        acceptedProposal.setProject(project);
        acceptedProposal.setFreelancer(freelancer);
        acceptedProposal.setProposedBudget(new BigDecimal("750.00"));
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("700.00"));
        payment.setProject(project);
        when(proposalRepository.findByStatus(Proposal.Status.ACCEPTED)).thenReturn(
                java.util.List.of(acceptedProposal));
        when(hiringRequestRepository.findByStatus(HiringRequest.Status.ACCEPTED)).thenReturn(
                java.util.List.of());
        when(projectRepository.findAll()).thenReturn(java.util.List.of(project));
        when(paymentRepository.findByProjectIds(java.util.List.of(7L))).thenReturn(
                java.util.List.of(payment));

        var responses = adminService.getProjects();

        assertEquals(new BigDecimal("700.00"), responses.get(0).freelancerPrice());
    }

    @Test
    void deleteUserRejectsAdminAccounts() {
        User admin = new User();
        admin.setRole(User.Role.ADMIN);
        when(userRepository.findById(12L)).thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> adminService.deleteUser(12L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(userRepository, never()).delete(admin);
    }

    @Test
    void deleteUserReturnsNotFoundWhenMissing() {
        when(userRepository.findById(12L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> adminService.deleteUser(12L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void deleteProjectRemovesAllRelatedRecordsBeforeProject() {
        Project project = new Project();
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        adminService.deleteProject(7L);

        var order = inOrder(paymentRepository, proposalRepository, hiringRequestRepository, projectRepository);
        order.verify(paymentRepository).deleteByProjectId(7L);
        order.verify(proposalRepository).deleteByProjectId(7L);
        order.verify(hiringRequestRepository).deleteByProjectId(7L);
        order.verify(projectRepository).delete(project);
    }

    @Test
    void deleteProjectReturnsNotFoundWithoutDeletingChildrenWhenMissing() {
        when(projectRepository.findById(7L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class, () -> adminService.deleteProject(7L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(paymentRepository, never()).deleteByProjectId(7L);
        verify(proposalRepository, never()).deleteByProjectId(7L);
        verify(hiringRequestRepository, never()).deleteByProjectId(7L);
    }
}
