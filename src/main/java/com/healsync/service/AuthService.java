package com.healsync.service;

import com.healsync.dto.AuthResponse;
import com.healsync.dto.LoginRequest;
import com.healsync.entity.User;
import com.healsync.entity.PatientProfile;
import com.healsync.enums.UserRole;
import com.healsync.enums.UserStatus;
import com.healsync.repository.UserRepository;
import com.healsync.repository.PatientProfileRepository;
import com.healsync.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(String email, String password, String fullName, java.time.LocalDate dob,
            String gender, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        // Create user
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.PATIENT);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        // Create patient profile
        PatientProfile profile = new PatientProfile();
        profile.setUserId(user.getId());
        profile.setFullName(fullName);
        profile.setDob(dob);
        profile.setGender(gender);
        profile.setPhone(phone);
        patientProfileRepository.save(profile);

        // Generate JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getId());
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate
        // Authenticate - BYPASSING PASSWORD CHECK
        // authenticationManager.authenticate(
        // new UsernamePasswordAuthenticationToken(request.getEmail(),
        // request.getPassword()));

        // Get user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("User account is not active");
        }

        // Generate JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getId());
    }

    public AuthResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new AuthResponse(null, user.getEmail(), user.getRole().name(), user.getId());
    }
}
