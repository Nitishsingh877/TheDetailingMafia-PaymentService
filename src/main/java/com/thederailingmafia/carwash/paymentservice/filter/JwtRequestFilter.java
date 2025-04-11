package com.thederailingmafia.carwash.paymentservice.filter;



import com.thederailingmafia.carwash.paymentservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        System.out.println("Raw Authorization header: " + authHeader);

        String email = null;
        String jwtToken = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);
            System.out.println("Extracted token: " + jwtToken);
            try {
                email = jwtUtil.getEmailFromToken(jwtToken);
                System.out.println("Extracted email: " + email);
            } catch (Exception e) {
                System.out.println("Failed to extract email from token: " + e.getMessage());
            }
        } else {
            System.out.println("No valid Bearer token in header");
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            System.out.println("Loaded UserDetails: " + (userDetails != null ? userDetails.getUsername() + ", " + userDetails.getAuthorities() : "null"));

            if (userDetails != null && jwtUtil.validateToken(jwtToken, email)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Set Authentication in context for: " + email + ", Authorities: " + userDetails.getAuthorities());
            } else {
                System.out.println("Token validation failed for email: " + email);
            }
        } else if (email != null) {
            System.out.println("Authentication already exists in context");
        }

        filterChain.doFilter(request, response);
    }
}
