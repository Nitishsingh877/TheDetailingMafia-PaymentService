package com.thederailingmafia.carwash.paymentservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    // Must match UserService's signing key
    private final String SECRET_KEY = "a-string-secret-at-least-256-bits-long-nitishsinghrajput"; // Replace with actual key
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    public String getEmailFromToken(String token) {
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(key) // Set the verification key here
                    .build();

            Claims claims = parser.parseClaimsJws(token).getBody();
            System.out.println("JWT Claims: " + claims);
            return claims.getSubject();


        } catch (Exception e) {
            System.out.println("Failed to parse token: " + e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(String token, String email) {
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(key) // Set the verification key here
                    .build();

            Claims claims = parser.parseClaimsJws(token).getBody();
            // Allow some clock skew (e.g., 5 minutes)
            Date expiration = claims.getExpiration();
            Date now = new Date();
            long skewMillis = 5 * 60 * 1000; // 5 minutes
            if (expiration != null && expiration.before(new Date(now.getTime() - skewMillis))) {
                System.out.println("Token expired: exp=" + expiration + ", now=" + now);
                return false;
            }

            // Verify email
            String tokenEmail = claims.getSubject();
            if (!email.equals(tokenEmail)) {
                System.out.println("Email mismatch: expected=" + email + ", got=" + tokenEmail);
                return false;
            }

            // Verify role
            String role = claims.get("role", String.class);
            if (!"WASHER".equals(role)) {
                System.out.println("Invalid role: " + role);
                return false;
            }

            System.out.println("Token validated: email=" + email + ", role=" + role);
            return true;
        } catch (Exception e) {
            System.out.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }
}