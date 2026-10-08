package com.freelancer.dto;

import com.freelancer.model.User;

public record AccountProfileResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String bio,
        User.Role role
) {
    public static AccountProfileResponse from(User user) {
        return new AccountProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getBio(),
                user.getRole()
        );
    }

    public static AccountProfileResponse from(User user, String phone, String bio) {
        return new AccountProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                phone,
                bio,
                user.getRole()
        );
    }
}
