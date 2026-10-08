package com.freelancer.controller;

import com.freelancer.dto.ProjectResponse;
import com.freelancer.model.Project;
import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.UserRepository;
import com.freelancer.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    void deleteMissingUserReturnsJson404() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "User not found"))
                .when(adminService).deleteUser(12L);

        mockMvc.perform(delete("/api/admin/users/12"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void deleteAdminUserReturnsJson403() throws Exception {
        doThrow(new ResponseStatusException(FORBIDDEN, "Admin accounts cannot be deleted here"))
                .when(adminService).deleteUser(12L);

        mockMvc.perform(delete("/api/admin/users/12"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Admin accounts cannot be deleted here"));
    }

    @Test
    void blockedUserDeleteReturnsJson409() throws Exception {
        doThrow(new DataIntegrityViolationException("foreign key constraint"))
                .when(adminService).deleteUser(12L);

        mockMvc.perform(delete("/api/admin/users/12"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(
                        "Cannot delete this record because related records still reference it"));
    }

    @Test
    void projectDeleteReturnsOkAfterRelatedRecordsAreRemoved() throws Exception {
        mockMvc.perform(delete("/api/admin/projects/7"))
                .andExpect(status().isOk());
    }

    @Test
    void getProjectsReturnsFreelancerName() throws Exception {
        when(adminService.getProjects()).thenReturn(List.of(new ProjectResponse(
                7L, "Website design", "Client Name", "Freelancer Name",
                new java.math.BigDecimal("1000.00"), new java.math.BigDecimal("750.00"),
                Project.Status.IN_PROGRESS)));

        mockMvc.perform(get("/api/admin/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientName").value("Client Name"))
                .andExpect(jsonPath("$[0].freelancerName").value("Freelancer Name"))
                .andExpect(jsonPath("$[0].clientBudget").value(1000.0))
                .andExpect(jsonPath("$[0].freelancerPrice").value(750.0));
    }

    @Test
    void missingProjectDeleteReturnsJson404() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Project not found"))
                .when(adminService).deleteProject(7L);

        mockMvc.perform(delete("/api/admin/projects/7"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Project not found"));
    }
}
