package com.sipapp.controller;

import com.sipapp.dto.AuthDto;
import com.sipapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Signup and Login APIs")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user and auto-generate passbook ID")
    public ResponseEntity<AuthDto.SignupResponse> signup(@Valid @RequestBody AuthDto.SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
