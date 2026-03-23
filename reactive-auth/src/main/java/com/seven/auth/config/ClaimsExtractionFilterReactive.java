package com.seven.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(ServerHttpRequest.class)
public class ClaimsExtractionFilterReactive implements WebFilter {
    private final Logger log = LoggerFactory.getLogger(getClass());


    @Override
    public reactor.core.publisher.Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AccountAuthenticationToken token) {
            // Case: Resource Server (JWT Bearer Token)
            Map<String, Object> attributes = exchange.getRequest().getAttributes();
            attributes.put("principal", token.getPrincipal());
            attributes.put("tenant", token.getTenant());
            attributes.put("permissions", token.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
        }

        return chain.filter(exchange);
    }
}