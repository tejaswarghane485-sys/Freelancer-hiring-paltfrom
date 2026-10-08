package com.freelancer.service;

import com.freelancer.dto.HiringRequestDto;
import com.freelancer.model.HiringRequest;
import com.freelancer.model.Project;
import com.freelancer.model.User;
import com.freelancer.repository.HiringRequestRepository;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HiringRequestServiceTest {

    @Mock
    private HiringRequestRepository hiringRequestRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HiringRequestService hiringRequestService;

    @Test
    void sendingHiringRequestCreatesPendingRequestWithoutActivatingProject() {
        User client = new User();
        client.setId(5L);
        User freelancer = new User();
        freelancer.setId(9L);
        freelancer.setRole(User.Role.FREELANCER);
        Project project = new Project();
        project.setId(20L);
        project.setClient(client);
        project.setBudget(new BigDecimal("450.00"));

        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(hiringRequestRepository.existsByProjectIdAndFreelancerId(20L, 9L)).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(client));
        when(userRepository.findById(9L)).thenReturn(Optional.of(freelancer));
        when(hiringRequestRepository.save(any(HiringRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        HiringRequestDto dto = new HiringRequestDto();
        dto.setProjectId(20L);
        dto.setFreelancerId(9L);
        dto.setMessage("Let's work together.");

        HiringRequest request = hiringRequestService.sendHiringRequest(5L, dto);

        assertEquals(HiringRequest.Status.PENDING, request.getStatus());
        assertEquals(Project.Status.OPEN, project.getStatus());
        org.mockito.Mockito.verifyNoInteractions(paymentService);
        org.mockito.Mockito.verify(projectRepository, org.mockito.Mockito.never()).save(project);
    }

    @Test
    void newHiringRequestsDefaultToPending() {
        HiringRequest request = new HiringRequest();
        assertEquals(HiringRequest.Status.PENDING, request.getStatus());
    }

    @Test
    void acceptingHiringRequestDoesNotStartProjectOrCreateInvoice() {
        User client = new User();
        client.setId(5L);
        User freelancer = new User();
        freelancer.setId(9L);

        Project project = new Project();
        project.setId(20L);
        project.setClient(client);
        project.setBudget(new BigDecimal("450.00"));

        HiringRequest request = new HiringRequest();
        request.setId(30L);
        request.setProject(project);
        request.setClient(client);
        request.setFreelancer(freelancer);
        request.setStatus(HiringRequest.Status.PENDING);

        when(hiringRequestRepository.findById(30L)).thenReturn(Optional.of(request));
        when(projectRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(project));
        when(hiringRequestRepository.save(request)).thenReturn(request);

        HiringRequest result = hiringRequestService.acceptRequest(30L, 9L);

        assertEquals(HiringRequest.Status.ACCEPTED, result.getStatus());
        assertEquals(Project.Status.OPEN, project.getStatus());
        org.mockito.Mockito.verifyNoInteractions(paymentService);
        org.mockito.Mockito.verify(projectRepository, org.mockito.Mockito.never()).save(project);
    }

    @Test
    void rejectingHiringRequestUpdatesStatusWithoutActivatingProject() {
        User freelancer = new User();
        freelancer.setId(9L);
        User client = new User();
        client.setId(5L);
        Project project = new Project();
        project.setId(20L);
        project.setClient(client);
        HiringRequest request = new HiringRequest();
        request.setId(30L);
        request.setProject(project);
        request.setFreelancer(freelancer);
        request.setStatus(HiringRequest.Status.PENDING);
        when(hiringRequestRepository.findById(30L)).thenReturn(Optional.of(request));
        when(hiringRequestRepository.save(request)).thenReturn(request);

        HiringRequest result = hiringRequestService.rejectRequest(30L, 9L);

        assertEquals(HiringRequest.Status.REJECTED, result.getStatus());
        org.mockito.Mockito.verifyNoInteractions(projectRepository, paymentService);
    }
}
