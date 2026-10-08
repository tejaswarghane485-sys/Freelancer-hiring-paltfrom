package com.freelancer.controller;

import com.freelancer.dto.AccountProfileRequest;
import com.freelancer.dto.AccountProfileResponse;
import com.freelancer.dto.AccountProfileUpdateResponse;
import com.freelancer.dto.ChangePasswordRequest;
import com.freelancer.service.AccountService;
import com.freelancer.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping({"/api/account", "/api/users"})
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.OPTIONS})
public class AccountController {

    private static final String USER_ID_ATTRIBUTE = "userId";

    private final AccountService accountService;
    private final JwtUtil jwtUtil;

    public AccountController(AccountService accountService, JwtUtil jwtUtil) {
        this.accountService = accountService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/profile")
    public ResponseEntity<AccountProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ResponseEntity.ok(accountService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<Object> updateProfile(@Valid @RequestBody AccountProfileRequest profile,
                                                HttpServletRequest request) {
        try {
            Long userId = requireUserId(request);
            var saved = accountService.updateProfile(userId, profile);
            String token = jwtUtil.generateToken(saved.getEmail(), saved.getRole().name(), saved.getId());
            return ResponseEntity.ok(new AccountProfileUpdateResponse(
                    AccountProfileResponse.from(saved), token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest password,
                                                              HttpServletRequest request) {
        try {
            Long userId = requireUserId(request);
            accountService.changePassword(userId, password);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userId;
    }
}
