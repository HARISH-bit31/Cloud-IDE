package com.cloudide.cloudide.service;

import com.cloudide.cloudide.dto.AuthUserResponse;
import com.cloudide.cloudide.dto.LoginRequest;
import com.cloudide.cloudide.dto.LoginResponse;
import com.cloudide.cloudide.dto.RegisterRequest;
import com.cloudide.cloudide.dto.UserRequest;
import com.cloudide.cloudide.dto.UserResponse;
import com.cloudide.cloudide.entity.User;
import com.cloudide.cloudide.exception.ConflictException;
import com.cloudide.cloudide.exception.ResourceNotFoundException;
import com.cloudide.cloudide.mapper.DtoMapper;
import com.cloudide.cloudide.repository.UserRepository;
import com.cloudide.cloudide.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthUserResponse registerUser(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user with id: {} and email: {}", saved.getId(), saved.getEmail());
        return DtoMapper.toAuthUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse authenticateUser(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (Exception e) {
            log.warn("Authentication failed for email: {}", email);
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = jwtService.generateToken(user.getEmail());
        return LoginResponse.builder()
                .token(token)
                .user(DtoMapper.toAuthUserResponse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("No authenticated user found in security context");
        }

        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String principalString) {
            email = principalString;
        } else {
            throw new BadCredentialsException("Invalid user principal in security context");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUserResponse() {
        User user = getCurrentAuthenticatedUser();
        return DtoMapper.toAuthUserResponse(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(DtoMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return DtoMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("User already exists with email: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        log.info("Created user with id: {} and email: {}", saved.getId(), saved.getEmail());
        return DtoMapper.toUserResponse(saved);
    }

    @Transactional
    public User getOrCreateDefaultUser() {
        return userRepository.findByEmail("alex.developer@cloud-ide.io")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Alex Developer")
                        .email("alex.developer@cloud-ide.io")
                        .password(passwordEncoder.encode("password123"))
                        .build()));
    }
}
