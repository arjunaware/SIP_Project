package com.sipapp.service;

import com.sipapp.config.JwtUtil;
import com.sipapp.dto.AuthDto;
import com.sipapp.entity.Passbook;
import com.sipapp.entity.User;
import com.sipapp.exception.BadRequestException;
import com.sipapp.repository.PassbookRepository;
import com.sipapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private PassbookRepository passbookRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    @Transactional
    public AuthDto.SignupResponse signup(AuthDto.SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        String passbookId = generatePassbookId();
        Passbook passbook = Passbook.builder()
                .id(passbookId)
                .user(user)
                .build();
        passbookRepository.save(passbook);

        AuthDto.SignupResponse response = new AuthDto.SignupResponse();
        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPassbookId(passbookId);
        response.setMessage("Account created successfully. Your passbook ID is: " + passbookId);
        return response;
    }

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String passbookId = passbookRepository.findByUserId(user.getId())
                .stream().findFirst()
                .map(Passbook::getId)
                .orElse(null);

        AuthDto.LoginResponse response = new AuthDto.LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPassbookId(passbookId);
        return response;
    }

    private String generatePassbookId() {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        Random random = new Random();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(letters.charAt(random.nextInt(letters.length())));
        }
        for (int i = 0; i < 3; i++) {
            sb.append(digits.charAt(random.nextInt(digits.length())));
        }

        String id = sb.toString();
        // Ensure uniqueness
        if (passbookRepository.existsById(id)) {
            return generatePassbookId();
        }
        return id;
    }
}
