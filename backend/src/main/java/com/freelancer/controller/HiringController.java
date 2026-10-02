package com.freelancer.controller;

import com.freelancer.dto.HiringRequestDto;
import com.freelancer.model.HiringRequest;
import com.freelancer.service.HiringRequestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hiring")
public class HiringController {

    @Autowired private HiringRequestService hiringRequestService;

    @PostMapping
    public ResponseEntity<?> sendHiringRequest(@RequestBody HiringRequestDto dto,
                                                HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            HiringRequest request = hiringRequestService.sendHiringRequest(userId, dto);
            return ResponseEntity.ok(request);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            HiringRequest request = hiringRequestService.respondToRequest(id, userId, body.get("status"));
            return ResponseEntity.ok(request);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/received")
    public ResponseEntity<List<HiringRequest>> getReceivedRequests(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(hiringRequestService.getRequestsForFreelancer(userId));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<HiringRequest>> getSentRequests(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(hiringRequestService.getRequestsByClient(userId));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<HiringRequest>> getRequestsForProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(hiringRequestService.getRequestsForProject(projectId));
    }
}

