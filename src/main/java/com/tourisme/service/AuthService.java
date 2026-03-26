package com.tourisme.service;

import com.tourisme.dto.request.LoginRequest;
import com.tourisme.dto.request.RegisterRequest;
import com.tourisme.dto.response.AuthResponse;
import com.tourisme.dto.response.UserResponse;
import com.tourisme.entity.User;
import com.tourisme.exception.BadRequestException;
import com.tourisme.exception.DuplicateResourceException;
import com.tourisme.mapper.UserMapper;
import com.tourisme.repository.UserRepository;
import com.tourisme.security.CustomUserDetailsService;
import com.tourisme.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final GoogleOAuthService googleOAuthService;
    private final CustomUserDetailsService userDetailsService;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }
        
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.ROLE_CLIENT)
                .active(true)
                .build();
        
        user = userRepository.save(user);
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        
        String token = tokenProvider.generateToken(authentication);
        UserResponse userResponse = userMapper.toResponse(user);
        
        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }
    
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String token = tokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        UserResponse userResponse = userMapper.toResponse(user);
        
        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }
    
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        return userMapper.toResponse(user);
    }
    
    @Transactional
    public AuthResponse googleAuth(String accessToken) {
        GoogleOAuthService.GoogleUserProfile profile = googleOAuthService.fetchVerifiedProfile(accessToken);
        
        User user = userRepository.findByEmail(profile.email())
                .orElseGet(() -> userRepository.save(buildUserFromGoogleProfile(profile)));
        
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("Account is disabled");
        }
        
        UserDetails principal = userDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        String token = tokenProvider.generateToken(authentication);
        return AuthResponse.builder()
                .token(token)
                .user(userMapper.toResponse(user))
                .build();
    }
    
    private User buildUserFromGoogleProfile(GoogleOAuthService.GoogleUserProfile profile) {
        String firstName = profile.givenName() != null ? profile.givenName() : deriveFirstNameFromEmail(profile.email());
        String lastName = profile.familyName() != null ? profile.familyName() : "";
        if (firstName.length() > 100) {
            firstName = firstName.substring(0, 100);
        }
        if (lastName.length() > 100) {
            lastName = lastName.substring(0, 100);
        }
        return User.builder()
                .firstName(firstName)
                .lastName(lastName.isEmpty() ? "-" : lastName)
                .email(profile.email())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(User.Role.ROLE_CLIENT)
                .active(true)
                .build();
    }
    
    private static String deriveFirstNameFromEmail(String email) {
        int at = email.indexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        return local.isBlank() ? "User" : local;
    }
}
