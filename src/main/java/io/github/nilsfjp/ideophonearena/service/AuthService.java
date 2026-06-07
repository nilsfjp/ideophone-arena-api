package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.AuthResponse;
import io.github.nilsfjp.ideophonearena.dto.LoginRequest;
import io.github.nilsfjp.ideophonearena.dto.RegisterRequest;
import io.github.nilsfjp.ideophonearena.exception.AuthenticationFailedException;
import io.github.nilsfjp.ideophonearena.exception.ConflictException;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (appUserRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }
        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        AppUser user = new AppUser(username, email, passwordEncoder.encode(request.getPassword()));
        AppUser savedUser = appUserRepository.save(user);
        return toAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid username or password");
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(AppUser user) {
        return new AuthResponse(jwtService.generateToken(user), user.getUsername(), user.getRole().name());
    }
}
