package com.freelancer.service;

import com.freelancer.dto.AuthRequest;
import com.freelancer.dto.AuthResponse;
import com.freelancer.dto.RegisterRequest;
import com.freelancer.model.User;
import com.freelancer.repository.UserRepository;
import com.freelancer.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerStoresEncodedPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jamie Client");
        request.setEmail("jamie@example.com");
        request.setPassword("password123");
        request.setRole("client");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(12L);
            return saved;
        });
        when(jwtUtil.generateToken("jamie@example.com", "CLIENT", 12L)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertAll(
                () -> assertEquals("jwt-token", response.getToken()),
                () -> assertEquals(12L, response.getUserId()),
                () -> assertEquals("Jamie Client", response.getFullName()),
                () -> assertEquals("jamie@example.com", response.getEmail()),
                () -> assertEquals("CLIENT", response.getRole())
        );
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerRejectsAnAlreadyRegisteredEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginReturnsTokenWhenPasswordMatches() {
        AuthRequest request = new AuthRequest();
        request.setEmail("jamie@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(12L);
        user.setFullName("Jamie Client");
        user.setEmail(request.getEmail());
        user.setPassword("encoded-password");
        user.setRole(User.Role.CLIENT);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail(), "CLIENT", 12L)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("CLIENT", response.getRole());
    }

    @Test
    void loginRejectsUnknownEmailAndIncorrectPassword() {
        AuthRequest request = new AuthRequest();
        request.setEmail("missing@example.com");
        request.setPassword("password123");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        Executable unknownEmail = () -> authService.login(request);
        assertThrows(RuntimeException.class, unknownEmail);

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword("encoded-password");
        user.setRole(User.Role.CLIENT);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }
}
