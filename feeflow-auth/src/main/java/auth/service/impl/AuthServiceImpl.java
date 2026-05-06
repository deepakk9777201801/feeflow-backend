package auth.service.impl;

import auth.entity.Institute;
import auth.entity.Role;
import auth.entity.User;
import auth.entity.UserRole;
import auth.repository.InstituteRepository;
import auth.repository.UserRepository;
import auth.security.JwtService;
import auth.service.AuthService;
import auth.service.email.EmailService;
import common.dto.auth.AuthResponse;
import common.dto.auth.LoginRequest;
import common.dto.auth.RegisterRequest;
import common.dto.auth.ResetPasswordRequest;
import common.exception.ResourceNotFoundException;
import common.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final InstituteRepository instituteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed. Email already registered: {}", request.getEmail());
            throw new UserAlreadyExistsException("Email is already registered: " + request.getEmail());
        }

        // Find or associate institute (Optional, depends on business logic)
        Institute institute = null;
        if (request.getInstituteId() != null) {
            log.debug("Fetching institute with ID: {}", request.getInstituteId());
            institute = instituteRepository.findById(request.getInstituteId())
                    .orElseThrow(() -> {
                        log.warn("Registration failed. Institute not found with ID: {}", request.getInstituteId());
                        return new ResourceNotFoundException("Institute not found with id: " + request.getInstituteId());
                    });
        }

        // Parse Role
        Role roleToAssign = Role.STUDENT; // Default
        if (request.getRole() != null) {
            try {
                roleToAssign = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided: {}. Defaulting to STUDENT.", request.getRole());
            }
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .institute(institute)
                .roles(new ArrayList<>())
                .build();

        UserRole userRole = UserRole.builder()
                .user(user)
                .institute(institute)
                .role(roleToAssign)
                .build();
        user.getRoles().add(userRole);

        userRepository.save(user);
        log.info("User registered successfully with email: {} and role: {}", request.getEmail(), roleToAssign);

        auth.security.CustomUserDetails customUserDetails = new auth.security.CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(customUserDetails);
        return buildAuthResponse(jwtToken, user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Authentication succeeded but user not found in DB: {}", request.getEmail());
                    return new ResourceNotFoundException("User not found with email: " + request.getEmail());
                });

        log.info("Login successful for user: {}", request.getEmail());

        auth.security.CustomUserDetails customUserDetails = new auth.security.CustomUserDetails(user);
        String jwtToken = jwtService.generateToken(customUserDetails);
        return buildAuthResponse(jwtToken, user);
    }

    @Override
    public AuthResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Fetching current user details for: {}", email);

        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return buildAuthResponse(null, user); // Token is not needed for the 'me' response typically
    }

    @Override
    public String logout() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("User logged out: {}", email);

        // JWT is stateless, so "logout" implies client-side deletion of token
        SecurityContextHolder.clearContext();
        return "Logged out successfully";
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        List<String> rolesStr = user.getRoles().stream()
                .map(r -> r.getRole().name())
                .toList();

        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .instituteId(user.getInstitute() != null ? user.getInstitute().getId() : null)
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roles(rolesStr)
                .build();

        return AuthResponse.builder()
                .token(token)
                .user(userDto)
                .build();
    }
    @Override
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);
        emailService.sendOtp(email, otp);

        return "OTP sent to email";
    }

    @Override
    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password mismatch");
        }

        if (user.getResetOtp() == null || !user.getResetOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setResetOtp(null);
        user.setOtpExpiry(null);

        userRepository.save(user);
        return "Password reset successful";
    }

    private String generateOtp() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }
}
