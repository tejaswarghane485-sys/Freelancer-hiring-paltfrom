package com.freelancer.service;

import com.freelancer.dto.ProjectRequest;
import com.freelancer.model.HiringRequest;
import com.freelancer.model.Proposal;
import com.freelancer.model.Project;
import com.freelancer.model.User;
import com.freelancer.repository.HiringRequestRepository;
import com.freelancer.repository.PaymentRepository;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HiringRequestRepository hiringRequestRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProjectSetsClientDetailsAndDefaultsToOpen() {
        User client = new User();
        client.setId(4L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(client));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectRequest request = projectRequest();
        Project saved = projectService.createProject(4L, request);

        assertEquals(client, saved.getClient());
        assertEquals("Website redesign", saved.getTitle());
        assertEquals(new BigDecimal("2500.00"), saved.getBudget());
        assertEquals(LocalDate.of(2026, 12, 1), saved.getDeadline());
        assertEquals(Project.Status.OPEN, saved.getStatus());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProjectRejectsUnknownClient() {
        when(userRepository.findById(4L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> projectService.createProject(4L, projectRequest()));

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void keywordSearchUsesOpenProjectsOnlyRepositoryQuery() {
        List<Project> openProjects = List.of(new Project());
        when(projectRepository.findOpenProjectsByKeyword("design")).thenReturn(openProjects);

        assertEquals(openProjects, projectService.searchByKeyword("design"));

        verify(projectRepository).findOpenProjectsByKeyword("design");
    }

    @Test
    void updateAndDeleteRequireProjectOwnership() {
        Project project = new Project();
        User owner = new User();
        owner.setId(4L);
        project.setClient(owner);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        assertThrows(RuntimeException.class, () -> projectService.updateProject(7L, 9L, projectRequest()));
        assertThrows(RuntimeException.class, () -> projectService.deleteProject(7L, 9L));

        verify(projectRepository, never()).save(any(Project.class));
        verify(projectRepository, never()).delete(any(Project.class));
        verify(paymentRepository, never()).deleteByProjectId(7L);
        verify(hiringRequestRepository, never()).deleteByProjectId(7L);
        verify(proposalRepository, never()).deleteByProjectId(7L);
    }

    @Test
    void deletePaidProjectRemovesTransactionAndDependentRecordsBeforeProject() {
        Project project = new Project();
        User owner = new User();
        owner.setId(4L);
        project.setClient(owner);
        project.setPaymentStatus(Project.PaymentStatus.PAID);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        projectService.deleteProject(7L, 4L);

        var deletionOrder = inOrder(paymentRepository, proposalRepository, hiringRequestRepository, projectRepository);
        deletionOrder.verify(paymentRepository).deleteByProjectId(7L);
        deletionOrder.verify(proposalRepository).deleteByProjectId(7L);
        deletionOrder.verify(hiringRequestRepository).deleteByProjectId(7L);
        deletionOrder.verify(projectRepository).delete(project);
    }

    @Test
    void updateStatusChangesStatusForOwningClient() {
        Project project = new Project();
        User owner = new User();
        owner.setId(4L);
        project.setClient(owner);
        project.setStatus(Project.Status.SUBMITTED);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        Project updated = projectService.updateStatus(7L, 4L, "completed");

        assertEquals(Project.Status.COMPLETED, updated.getStatus());
    }

    @Test
    void updateStatusAllowsClientSelectableLifecycleStatuses() {
        Project project = new Project();
        User owner = new User();
        owner.setId(4L);
        project.setClient(owner);
        project.setStatus(Project.Status.IN_PROGRESS);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        for (Project.Status status : List.of(Project.Status.OPEN, Project.Status.IN_PROGRESS,
                Project.Status.COMPLETED, Project.Status.CLOSED)) {
            Project updated = projectService.updateStatus(7L, 4L, status.name());
            assertEquals(status, updated.getStatus());
        }

        assertThrows(IllegalArgumentException.class, () -> projectService.updateStatus(7L, 4L, "SUBMITTED"));
        verify(projectRepository, org.mockito.Mockito.times(4)).save(project);
    }

    @Test
    void submitWorkRequiresAcceptedFreelancerAndInProgressProject() {
        Project project = new Project();
        project.setId(7L);
        project.setStatus(Project.Status.IN_PROGRESS);
        Proposal acceptedProposal = new Proposal();
        acceptedProposal.setStatus(Proposal.Status.ACCEPTED);
        when(projectRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(project));
        when(proposalRepository.findByProjectIdAndFreelancerId(7L, 9L)).thenReturn(Optional.of(acceptedProposal));
        when(hiringRequestRepository.existsByProjectIdAndFreelancerIdAndStatus(
                7L, 9L, HiringRequest.Status.ACCEPTED)).thenReturn(false);
        when(projectRepository.save(project)).thenReturn(project);

        Project submitted = projectService.submitWork(7L, 9L);

        assertEquals(Project.Status.SUBMITTED, submitted.getStatus());
        verify(projectRepository).save(project);
    }

    @Test
    void submitWorkRejectsFreelancerWithoutAnAcceptedAssignment() {
        Project project = new Project();
        project.setId(7L);
        project.setStatus(Project.Status.IN_PROGRESS);
        when(projectRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(project));
        when(proposalRepository.findByProjectIdAndFreelancerId(7L, 9L)).thenReturn(Optional.empty());
        when(hiringRequestRepository.existsByProjectIdAndFreelancerIdAndStatus(
                7L, 9L, HiringRequest.Status.ACCEPTED)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> projectService.submitWork(7L, 9L));

        verify(projectRepository, never()).save(project);
    }

    @Test
    void updateProjectChangesStatusWhenProvided() {
        Project project = new Project();
        project.setId(7L);
        User owner = new User();
        owner.setId(4L);
        project.setClient(owner);
        project.setStatus(Project.Status.OPEN);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(proposalRepository.findByProjectId(7L)).thenReturn(List.of(new Proposal()));
        when(projectRepository.save(project)).thenReturn(project);

        ProjectRequest request = projectRequest();
        request.setStatus("IN_PROGRESS");

        Project updated = projectService.updateProject(7L, 4L, request);

        assertEquals(Project.Status.IN_PROGRESS, updated.getStatus());
    }

    @Test
    void updateStatusCanStartAProjectWithoutChangingItsProposalLifecycle() {
        User owner = new User();
        owner.setId(4L);
        Project project = new Project();
        project.setId(7L);
        project.setClient(owner);
        project.setStatus(Project.Status.OPEN);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        Project updated = projectService.updateStatus(7L, 4L, "IN_PROGRESS");

        assertEquals(Project.Status.IN_PROGRESS, updated.getStatus());
        verify(projectRepository).save(project);
    }

    @Test
    void updateProjectPreservesStatusWhenNotProvided() {
        Project project = new Project();
        User owner = new User();
        owner.setId(4L);
        project.setClient(owner);
        project.setStatus(Project.Status.COMPLETED);
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        Project updated = projectService.updateProject(7L, 4L, projectRequest());

        assertEquals(Project.Status.COMPLETED, updated.getStatus());
    }

    private ProjectRequest projectRequest() {
        ProjectRequest request = new ProjectRequest();
        request.setTitle("Website redesign");
        request.setDescription("Redesign a small business website");
        request.setRequiredSkills("HTML, CSS");
        request.setBudget(new BigDecimal("2500.00"));
        request.setDeadline("2026-12-01");
        return request;
    }
}
