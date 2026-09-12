package com.cloudide.cloudide.controller;

import com.cloudide.cloudide.dto.AuthUserResponse;
import com.cloudide.cloudide.dto.LoginRequest;
import com.cloudide.cloudide.dto.LoginResponse;
import com.cloudide.cloudide.dto.RegisterRequest;
import com.cloudide.cloudide.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthUserResponse response = userService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.authenticateUser(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser() {
        AuthUserResponse response = userService.getCurrentUserResponse();
        return ResponseEntity.ok(response);
    }
}
