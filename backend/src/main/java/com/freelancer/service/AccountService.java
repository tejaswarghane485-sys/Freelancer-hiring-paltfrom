package com.freelancer.service;

import com.freelancer.dto.AccountProfileRequest;
import com.freelancer.dto.AccountProfileResponse;
import com.freelancer.dto.ChangePasswordRequest;
import com.freelancer.model.FreelancerProfile;
import com.freelancer.model.User;
import com.freelancer.repository.FreelancerProfileRepository;
import com.freelancer.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private static final String ACCOUNT_NOT_FOUND = "Account not found";

    private final UserRepository userRepository;
    private final FreelancerProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository,
                          FreelancerProfileRepository profileRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountProfileResponse getProfile(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        FreelancerProfile profile = user.getRole() == User.Role.FREELANCER
                ? profileRepository.findByUserId(userId).orElse(null)
                : null;
        String phone = user.getPhone() != null || profile == null ? user.getPhone() : profile.getPhone();
        String bio = user.getBio() != null || profile == null ? user.getBio() : profile.getBio();
        return AccountProfileResponse.from(user, phone, bio);
    }

    @Transactional
    public User updateProfile(long userId, AccountProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        user.setFullName(request.getFullName().trim());
        user.setPhone(normalize(request.getPhone()));
        user.setBio(normalize(request.getBio()));

        if (user.getRole() == User.Role.FREELANCER) {
            profileRepository.findByUserId(userId).ifPresent(profile -> {
                profile.setPhone(user.getPhone());
                profile.setBio(user.getBio());
                profileRepository.save(profile);
            });
        }
        return userRepository.save(user);
    }

    public void changePassword(long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
