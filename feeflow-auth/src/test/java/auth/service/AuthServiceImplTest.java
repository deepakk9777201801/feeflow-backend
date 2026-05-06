package auth.service;

import auth.entity.Institute;
import auth.entity.Role;
import auth.entity.User;
import auth.entity.UserRole;
import auth.repository.InstituteRepository;
import auth.repository.UserRepository;
import auth.security.JwtService;
import auth.service.impl.AuthServiceImpl;
import common.dto.auth.AuthResponse;
import common.dto.auth.LoginRequest;
import common.dto.auth.RegisterRequest;
import common.exception.ResourceNotFoundException;
import common.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private InstituteRepository instituteRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private Institute institute;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        institute = Institute.builder().id(1).name("Test Institute").build();

        List<UserRole> roles = new ArrayList<>();

        user = User.builder()
                .id(1)
                .name("Test User")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .institute(institute)
                .roles(roles)
                .build();

        roles.add(UserRole.builder().user(user).role(Role.STUDENT).build());

        registerRequest = RegisterRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .phone("1234567890")
                .instituteId(1)
                .role("STUDENT")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();
    }

    @Test
    void register_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(instituteRepository.findById(anyInt())).thenReturn(Optional.of(institute));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        // Using doReturn to avoid the generic type inference issue with any() in Mockito
        doReturn(user).when(userRepository).save(any(User.class));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(user.getName(), response.getUser().getName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_UserAlreadyExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_InstituteNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(instituteRepository.findById(anyInt())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmailWithRoles(anyString())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(user.getName(), response.getUser().getName());
    }

    @Test
    void login_UserNotFound() {
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByEmailWithRoles(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    void getCurrentUser_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmailWithRoles("test@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.getCurrentUser();

        assertNotNull(response);
        assertNull(response.getToken());
        assertEquals(user.getName(), response.getUser().getName());
    }

    @Test
    void getCurrentUser_UserNotFound() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmailWithRoles("test@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getCurrentUser());
    }

    @Test
    void logout_ClearsSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");

        String response = authService.logout();

        assertEquals("Logged out successfully", response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}