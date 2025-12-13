package com.seven.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.seven.auth.JwtService;
import com.seven.auth.account.AccountDTO;
import com.seven.auth.config.authentication.AuthVariant;
import com.seven.auth.config.threadlocal.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = request.getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7);
                    Claims claims = jwtService.extractClaims(token);
                    String email = claims.getSubject();

                    if (email != null) {
                        if (jwtService.isTokenValid(claims)) {
                            //Extract permissions
                            List<String> permissions = (List<String>)claims.get("permissions");
                            //Extract account record
                            AccountDTO.Record accountRecord = objectMapper.convertValue(claims.get("principal"), AccountDTO.Record.class);
                            //Extract tenant
                            String tenant = (String) claims.get("tenant");

                            request.setAttribute("subject", email);
                            request.setAttribute("permissions", permissions);

                            TenantContext.setCurrentAuthVariant(AuthVariant.JWT);
                            request.setAttribute("tenant", tenant);

                            UsernamePasswordAuthenticationToken authenticationToken =
                                    new UsernamePasswordAuthenticationToken(accountRecord, null, List.of());

                            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error authenticating user: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        try {
            filterChain.doFilter(request, response);
        }finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }
}
