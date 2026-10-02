package com.freelancer.controller;

import com.freelancer.dto.ProjectRequest;
import com.freelancer.model.Project;
import com.freelancer.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired private ProjectService projectService;

    // Public endpoint - all open projects
    @GetMapping("/open")
    public ResponseEntity<List<Project>> getOpenProjects() {
        return ResponseEntity.ok(projectService.getOpenProjects());
    }

    @PostMapping
    public ResponseEntity<?> createProject(@Valid @RequestBody ProjectRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            Project project = projectService.createProject(userId, request);
            return ResponseEntity.ok(project);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(projectService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<Project>> getMyProjects(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(projectService.getProjectsByClient(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id,
                                           @Valid @RequestBody ProjectRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            Project project = projectService.updateProject(id, userId, request);
            return ResponseEntity.ok(project);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            Project project = projectService.updateStatus(id, userId, body.get("status"));
            return ResponseEntity.ok(project);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            projectService.deleteProject(id, userId);
            return ResponseEntity.ok(Map.of("message", "Project deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Project>> search(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String skill) {
        if (skill != null && !skill.isBlank()) {
            return ResponseEntity.ok(projectService.searchBySkill(skill));
        }
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(projectService.searchByKeyword(keyword));
        }
        return ResponseEntity.ok(projectService.getOpenProjects());
    }
}

