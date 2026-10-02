package com.freelancer.controller;

import com.freelancer.dto.ProposalRequest;
import com.freelancer.model.Proposal;
import com.freelancer.service.ProposalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proposals")
public class ProposalController {

    @Autowired private ProposalService proposalService;

    @PostMapping
    public ResponseEntity<?> submitProposal(@Valid @RequestBody ProposalRequest request,
                                             HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            Proposal proposal = proposalService.submitProposal(userId, request);
            return ResponseEntity.ok(proposal);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Proposal>> getProposalsForProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(proposalService.getProposalsForProject(projectId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Proposal>> getMyProposals(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(proposalService.getProposalsByFreelancer(userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            Proposal proposal = proposalService.updateProposalStatus(id, userId, body.get("status"));
            return ResponseEntity.ok(proposal);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProposal(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            proposalService.deleteProposal(id, userId);
            return ResponseEntity.ok(Map.of("message", "Proposal deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

