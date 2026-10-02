package com.freelancer.controller;

import com.freelancer.dto.ProfileRequest;
import com.freelancer.model.FreelancerProfile;
import com.freelancer.service.FreelancerProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/freelancers")
public class FreelancerController {

    @Autowired private FreelancerProfileService profileService;

    @GetMapping
    public ResponseEntity<List<FreelancerProfile>> getAllFreelancers() {
        return ResponseEntity.ok(profileService.getAllProfiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFreelancerProfile(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(profileService.getProfileById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProfileByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(profileService.getProfileByUserId(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<?> saveProfile(@RequestBody ProfileRequest request,
                                         HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            FreelancerProfile profile = profileService.createOrUpdateProfile(userId, request);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<FreelancerProfile>> search(@RequestParam(required = false) String skill,
                                                           @RequestParam(required = false) String name) {
        if (skill != null && !skill.isBlank()) {
            return ResponseEntity.ok(profileService.searchBySkill(skill));
        }
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(profileService.searchByName(name));
        }
        return ResponseEntity.ok(profileService.getAllProfiles());
    }
}

