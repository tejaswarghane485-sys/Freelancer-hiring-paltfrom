package com.freelancer.service;

import com.freelancer.dto.AccountProfileRequest;
import com.freelancer.dto.ChangePasswordRequest;
import com.freelancer.model.FreelancerProfile;
import com.freelancer.model.User;
import com.freelancer.repository.FreelancerProfileRepository;
import com.freelancer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FreelancerProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    @Test
    void updateProfileSavesAccountAndSynchronizesFreelancerProfile() {
        User user = new User();
        user.setId(7L);
        user.setEmail("old@example.com");
        user.setRole(User.Role.FREELANCER);
        FreelancerProfile profile = new FreelancerProfile();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountProfileRequest request = new AccountProfileRequest();
        request.setFullName("  Alex Freelancer ");
        request.setPhone(" +91 1234567890 ");
        request.setBio("  Product designer ");

        accountService.updateProfile(7L, request);

        assertEquals("Alex Freelancer", user.getFullName());
        assertEquals("old@example.com", user.getEmail());
        assertEquals("+91 1234567890", user.getPhone());
        assertEquals("Product designer", user.getBio());
        assertEquals(user.getPhone(), profile.getPhone());
        assertEquals(user.getBio(), profile.getBio());
        verify(profileRepository).save(profile);
    }

    @Test
    void updateClientAccountAllowsMissingOptionalFieldsWithoutAccessingFreelancerProfiles() {
        User user = new User();
        user.setId(8L);
        user.setEmail("client@example.com");
        user.setRole(User.Role.CLIENT);
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountProfileRequest request = new AccountProfileRequest();
        request.setFullName("Client");

        accountService.updateProfile(8L, request);

        assertNull(user.getPhone());
        assertNull(user.getBio());
        verify(userRepository).save(user);
        verify(profileRepository, never()).findByUserId(8L);
    }

    @Test
    void getProfileReturnsNotFoundWhenAccountDoesNotExist() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accountService.getProfile(9L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Account not found", exception.getReason());
    }

    @Test
    void changePasswordVerifiesCurrentPasswordBeforeSavingEncodedPassword() {
        User user = new User();
        user.setId(7L);
        user.setPassword("old-hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("current");
        request.setNewPassword("new-password");
        accountService.changePassword(7L, request);

        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        User user = new User();
        user.setId(7L);
        user.setPassword("old-hash");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("new-password");

        assertThrows(IllegalArgumentException.class, () -> accountService.changePassword(7L, request));

        verify(userRepository, never()).save(any(User.class));
    }
}
