package com.project.projectmanagementapplication.security;


import com.project.projectmanagementapplication.model.User;
import com.project.projectmanagementapplication.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final JwtConfig jwtConfig;
    private final UserService userService;

    @Autowired
    public JwtUtil(JwtConfig jwtConfig, UserService userService) {
        this.jwtConfig = jwtConfig;
        this.userService = userService;
    }

    public String  generateToken(Authentication auth) {
        Map<String, Object> claims = new HashMap<>();
        String username = auth.getName();
        User user = userService.findByUsername(username);
        claims.put("userId", user.getId());
        return Jwts.builder()
                .claims(claims)
                .subject(auth.getName())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(jwtConfig.getSigningKey())
                .compact();

    }


    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtConfig.getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    public String extractUsername(String jwt) {
        return extractClaim(jwt, Claims::getSubject);
    }

    private boolean isTokenExpired(String jwt) {
        return extractClaim(jwt, Claims::getExpiration).before(new Date());
    }

    public boolean validateToken(String jwt, UserDetails userDetails) {
        String username = extractClaim(jwt, Claims::getSubject);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(jwt));

    }


}
