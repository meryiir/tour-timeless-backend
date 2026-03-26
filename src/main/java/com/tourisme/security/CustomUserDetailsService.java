package com.tourisme.security;

import com.tourisme.entity.User;
import com.tourisme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        Collection<? extends GrantedAuthority> authorities = getAuthorities(user);
        
        // Enhanced logging for debugging
        logger.info("Loading user: " + email);
        logger.info("User role: " + user.getRole());
        logger.info("User active: " + user.getActive());
        logger.info("Authorities: " + authorities);
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(!user.getActive())
                .credentialsExpired(false)
                .disabled(!user.getActive())
                .build();
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        String authority = user.getRole().name();
        logger.info("Creating authority: " + authority + " for user: " + user.getEmail());
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }
}
