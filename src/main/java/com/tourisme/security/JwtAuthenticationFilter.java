package com.tourisme.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (!StringUtils.hasText(jwt)) {
                logger.debug("No JWT token found in request: " + request.getMethod() + " " + request.getRequestURI());
                logger.debug("Authorization header: " + request.getHeader("Authorization"));
            } else if (tokenProvider.validateToken(jwt)) {
                logger.debug("JWT token validated successfully for request: " + request.getMethod() + " " + request.getRequestURI());
                String username = tokenProvider.getUsernameFromToken(jwt);
                logger.info("Extracted username from token: " + username + " for request: " + request.getMethod() + " " + request.getRequestURI());
                
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Enhanced logging for debugging
                logger.info("User loaded: " + username);
                logger.info("User enabled: " + userDetails.isEnabled());
                logger.info("User account non-locked: " + userDetails.isAccountNonLocked());
                logger.info("User authorities: " + userDetails.getAuthorities());
                logger.info("Authorities count: " + userDetails.getAuthorities().size());
                
                // Check if user account is enabled and not locked
                if (!userDetails.isEnabled()) {
                    logger.warn("User account is disabled: " + username);
                    // Don't set authentication - let it fail with 401
                } else if (!userDetails.isAccountNonLocked()) {
                    logger.warn("User account is locked: " + username);
                    // Don't set authentication - let it fail with 401
                } else {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Verify authentication was set
                    var contextAuth = SecurityContextHolder.getContext().getAuthentication();
                    if (contextAuth != null) {
                        logger.info("Authentication set in context. Principal: " + contextAuth.getName() + ", Authorities: " + contextAuth.getAuthorities());
                    } else {
                        logger.error("WARNING: Authentication was NOT set in SecurityContext!");
                    }
                }
            } else {
                logger.warn("JWT token validation failed for request: " + request.getMethod() + " " + request.getRequestURI());
                if (jwt != null) {
                    logger.warn("Token length: " + jwt.length());
                    logger.warn("Token preview (first 50 chars): " + (jwt.length() > 50 ? jwt.substring(0, 50) + "..." : jwt));
                    logger.warn("Token preview (last 20 chars): " + (jwt.length() > 20 ? "..." + jwt.substring(jwt.length() - 20) : jwt));
                } else {
                    logger.warn("Token is null");
                }
            }
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            logger.error("User not found while processing JWT token: " + ex.getMessage());
            // Don't set authentication - let it fail with 401
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context for request: " + 
                    request.getMethod() + " " + request.getRequestURI(), ex);
            logger.error("Exception details: ", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Authorization header value: " + (bearerToken != null ? (bearerToken.length() > 50 ? bearerToken.substring(0, 50) + "..." : bearerToken) : "null"));
        
        if (!StringUtils.hasText(bearerToken)) {
            logger.debug("Authorization header is empty or null");
            return null;
        }
        
        if (!bearerToken.startsWith("Bearer ")) {
            logger.warn("Authorization header does not start with 'Bearer ': " + (bearerToken.length() > 50 ? bearerToken.substring(0, 50) + "..." : bearerToken));
            return null;
        }
        
        String token = bearerToken.substring(7);
        logger.debug("Extracted JWT token, length: " + token.length());
        return token;
    }
}
