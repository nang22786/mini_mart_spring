package com.minimart.api.config;

import com.minimart.api.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        // Extract JWT token from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                email = jwtUtil.extractEmail(jwt);
            } catch (Exception e) {
                System.out.println("Invalid JWT token: " + e.getMessage());
            }
        }

        // Validate token and set authentication
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(jwt, email)) {
                String role = jwtUtil.extractRole(jwt);
                Long userId = jwtUtil.extractUserId(jwt);

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}