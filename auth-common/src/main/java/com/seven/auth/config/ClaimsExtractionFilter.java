package com.seven.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ClaimsExtractionFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AccountAuthenticationToken token) {
            // Case: Resource Server (JWT Bearer Token)
            request.setAttribute("principal", token.getPrincipal());
            request.setAttribute("tenant", token.getTenant());
            request.setAttribute("permissions", token.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
        }

        filterChain.doFilter(request, response);
    }
}