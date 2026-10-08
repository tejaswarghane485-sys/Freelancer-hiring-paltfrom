package com.freelancer.controller;

import com.freelancer.dto.HiringRequestDto;
import com.freelancer.dto.HiringRequestResponse;
import com.freelancer.model.HiringRequest;
import com.freelancer.service.HiringRequestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hiring-requests")
public class HiringController {

    @Autowired private HiringRequestService hiringRequestService;

    @PostMapping
    public ResponseEntity<?> sendHiringRequest(@RequestBody HiringRequestDto dto,
                                                HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            HiringRequest request = hiringRequestService.sendHiringRequest(userId, dto);
            return ResponseEntity.ok(HiringRequestResponse.from(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            HiringRequest request = hiringRequestService.acceptRequest(id, userId);
            return ResponseEntity.ok(HiringRequestResponse.from(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            HiringRequest request = hiringRequestService.rejectRequest(id, userId);
            return ResponseEntity.ok(HiringRequestResponse.from(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/received")
    public ResponseEntity<List<HiringRequestResponse>> getReceivedRequests(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(hiringRequestService.getRequestsForFreelancer(userId).stream()
                .map(HiringRequestResponse::from)
                .toList());
    }

    @GetMapping("/sent")
    public ResponseEntity<List<HiringRequestResponse>> getSentRequests(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(hiringRequestService.getRequestsByClient(userId).stream()
                .map(HiringRequestResponse::from)
                .toList());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<HiringRequestResponse>> getRequestsForProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(hiringRequestService.getRequestsForProject(projectId).stream()
                .map(HiringRequestResponse::from)
                .toList());
    }
}
