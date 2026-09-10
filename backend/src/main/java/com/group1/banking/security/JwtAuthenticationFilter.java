package com.group1.banking.security;

import com.group1.banking.dto.common.ErrorResponse;
import com.group1.banking.entity.User;
import com.group1.banking.repository.UserRepository;
import tools.jackson.databind.json.JsonMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final JsonMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, JsonMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            UUID userId = jwtService.extractUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                CustomUserPrincipal principal = new CustomUserPrincipal(user);
                
                
                System.out.println("Authenticated user: " + user.getUsername());
                System.out.println("Roles from DB: " + user.getRoles());
                System.out.println("Authorities: " + principal.getAuthorities());
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            writeError(response, 401, "SESSION_EXPIRED", "Session has expired.");
        } catch (Exception ex) {
            logger.error("JWT authentication failed: {}", ex);
            writeError(response, 401, "UNAUTHORISED", "Authentication failed.");
        }
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message, null));
    }
}
