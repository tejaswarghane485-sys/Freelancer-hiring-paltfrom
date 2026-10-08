package com.freelancer.config;

import com.freelancer.repository.ProjectRepository;
import com.freelancer.repository.ProposalRepository;
import com.freelancer.repository.UserRepository;
import com.freelancer.security.JwtAuthFilter;
import com.freelancer.security.JwtUtil;
import com.freelancer.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.freelancer.controller.AdminController.class,
        properties = {
                "cors.allowed-origins=http://localhost",
                "app.admin.email=admin@example.com"
        })
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private ProposalRepository proposalRepository;

    @MockBean
    private AdminService adminService;

    @BeforeEach
    void setUpJwtRoles() {
        when(jwtUtil.validateToken("client-token")).thenReturn(true);
        when(jwtUtil.extractRole("client-token")).thenReturn("CLIENT");
        when(jwtUtil.validateToken("freelancer-token")).thenReturn(true);
        when(jwtUtil.extractRole("freelancer-token")).thenReturn("FREELANCER");
        when(jwtUtil.validateToken("admin-token")).thenReturn(true);
        when(jwtUtil.extractRole("admin-token")).thenReturn("ADMIN");
        when(jwtUtil.extractEmail("client-token")).thenReturn("client@example.com");
        when(jwtUtil.extractEmail("freelancer-token")).thenReturn("freelancer@example.com");
        when(jwtUtil.extractEmail("admin-token")).thenReturn("admin@example.com");
        when(jwtUtil.extractUserId("client-token")).thenReturn(1L);
        when(jwtUtil.extractUserId("freelancer-token")).thenReturn(2L);
        when(jwtUtil.extractUserId("admin-token")).thenReturn(3L);
    }

    @Test
    void clientTokenIsForbiddenFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer client-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void freelancerTokenIsForbiddenFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer freelancer-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void adminTokenCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(0));
    }

    @Test
    void adminTokenForAnotherEmailIsForbidden() throws Exception {
        when(jwtUtil.extractEmail("other-admin-token")).thenReturn("other-admin@example.com");
        when(jwtUtil.extractRole("other-admin-token")).thenReturn("ADMIN");
        when(jwtUtil.extractUserId("other-admin-token")).thenReturn(4L);
        when(jwtUtil.validateToken("other-admin-token")).thenReturn(true);

        mockMvc.perform(get("/api/admin/stats").header("Authorization", "Bearer other-admin-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
