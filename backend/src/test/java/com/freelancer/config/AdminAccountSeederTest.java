package com.freelancer.config;

import com.freelancer.model.User;
import com.freelancer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsAdminWithEncodedDefaultPasswordWhenMissing() {
        AdminAccountSeeder seeder = new AdminAccountSeeder(userRepository, passwordEncoder);
        when(userRepository.findByEmail("rajshri@admin.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rajshri123")).thenReturn("encoded-password");

        seeder.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("rajshri@admin.com", saved.getEmail());
        assertEquals("FreelanceHub Admin", saved.getFullName());
        assertEquals(User.Role.ADMIN, saved.getRole());
        assertEquals("encoded-password", saved.getPassword());
    }

    @Test
    void encodesDefaultPasswordWhenCreatingAdmin() {
        AdminAccountSeeder seeder = new AdminAccountSeeder(userRepository, passwordEncoder);
        when(userRepository.findByEmail("rajshri@admin.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rajshri123")).thenReturn("encoded-default-password");

        seeder.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertEquals(User.Role.ADMIN, userCaptor.getValue().getRole());
        assertEquals("encoded-default-password", userCaptor.getValue().getPassword());
    }

    @Test
    void updatesExistingAccountToAdminAndResetsPassword() {
        User existing = new User();
        existing.setRole(User.Role.CLIENT);
        existing.setEmail("rajshri@admin.com");
        existing.setPassword("old-unhashed-password");
        AdminAccountSeeder seeder = new AdminAccountSeeder(userRepository, passwordEncoder);
        when(userRepository.findByEmail("rajshri@admin.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("rajshri123")).thenReturn("encoded-default-password");

        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(User.Role.ADMIN, existing.getRole());
        assertEquals("encoded-default-password", existing.getPassword());
        verify(userRepository).saveAndFlush(existing);
    }

    @Test
    void logsDatabaseFailureWithoutFailingStartup() {
        AdminAccountSeeder seeder = new AdminAccountSeeder(userRepository, passwordEncoder);
        when(userRepository.findByEmail("rajshri@admin.com"))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        seeder.run(new DefaultApplicationArguments(new String[0]));
    }
}
