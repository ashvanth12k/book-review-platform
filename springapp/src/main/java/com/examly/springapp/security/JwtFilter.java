package com.examly.springapp.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Safe logging — NEVER print the actual JWT token.
        System.out.println("Request URI: " + request.getRequestURI());

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {

                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);

                // Default role if JWT has no role.
                if (role == null || role.isBlank()) {
                    role = "ROLE_USER";
                }

                // Spring Security hasRole("PUBLISHER")
                // expects the authority ROLE_PUBLISHER.
                if (!role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                // Safe temporary debugging.
                // This does NOT print the JWT.
                System.out.println("=== JWT AUTH DEBUG ===");
                System.out.println("Authenticated email: " + email);
                System.out.println("JWT role: " + role);
                System.out.println("Granted authority: " + role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(role)
                                )
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

                System.out.println(
                        "Authentication stored in SecurityContext"
                );

            } else {
                System.out.println("JWT INVALID");
            }

        } else {
            System.out.println(
                    "Authorization header missing or not Bearer"
            );
        }

        filterChain.doFilter(request, response);
    }
}