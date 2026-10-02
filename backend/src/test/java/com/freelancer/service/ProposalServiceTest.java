package com.freelancer.service;

import com.freelancer.dto.ProposalRequest;
import com.freelancer.model.Project;
import com.freelancer.model.Proposal;
import com.freelancer.model.User;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProposalService proposalService;

    @Test
    void submitProposalCreatesPendingProposalForOpenProject() {
        Project project = new Project();
        project.setId(20L);
        project.setStatus(Project.Status.OPEN);
        User freelancer = new User();
        freelancer.setId(9L);

        when(proposalRepository.existsByProjectIdAndFreelancerId(20L, 9L)).thenReturn(false);
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(userRepository.findById(9L)).thenReturn(Optional.of(freelancer));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProposalRequest request = new ProposalRequest();
        request.setProjectId(20L);
        request.setCoverLetter("I can deliver this project.");
        request.setProposedBudget(new BigDecimal("1800.00"));
        request.setEstimatedDays(14);

        Proposal saved = proposalService.submitProposal(9L, request);

        assertEquals(project, saved.getProject());
        assertEquals(freelancer, saved.getFreelancer());
        assertEquals("I can deliver this project.", saved.getCoverLetter());
        assertEquals(Proposal.Status.PENDING, saved.getStatus());
    }

    @Test
    void submitProposalRejectsDuplicateProposal() {
        ProposalRequest request = new ProposalRequest();
        request.setProjectId(20L);
        when(proposalRepository.existsByProjectIdAndFreelancerId(20L, 9L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> proposalService.submitProposal(9L, request));

        verify(projectRepository, never()).findById(20L);
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void submitProposalRejectsProjectThatIsNotOpen() {
        Project project = new Project();
        project.setStatus(Project.Status.CLOSED);
        when(proposalRepository.existsByProjectIdAndFreelancerId(20L, 9L)).thenReturn(false);
        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));

        ProposalRequest request = new ProposalRequest();
        request.setProjectId(20L);

        assertThrows(RuntimeException.class, () -> proposalService.submitProposal(9L, request));

        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void updateProposalStatusRequiresOwningClient() {
        User owner = new User();
        owner.setId(4L);
        Project project = new Project();
        project.setClient(owner);
        Proposal proposal = new Proposal();
        proposal.setProject(project);
        when(proposalRepository.findById(30L)).thenReturn(Optional.of(proposal));

        assertThrows(RuntimeException.class, () -> proposalService.updateProposalStatus(30L, 5L, "accepted"));

        verify(proposalRepository, never()).save(proposal);
    }
}
