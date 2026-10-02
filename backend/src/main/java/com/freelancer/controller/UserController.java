package com.freelancer.controller;

import com.freelancer.model.User;
import com.freelancer.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    /** Returns the currently authenticated user's info */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Update display name for current user */
    @PatchMapping("/me")
    public ResponseEntity<?> updateName(@RequestBody Map<String, String> body,
                                        HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return userRepository.findById(userId).map(user -> {
            String newName = body.get("fullName");
            if (newName != null && !newName.isBlank()) {
                user.setFullName(newName.trim());
                userRepository.save(user);
            }
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }
}
