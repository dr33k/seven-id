package com.seven.auth.client.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthorizationFilter implements WebFilter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final List<String> permittedPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthorizationFilter(@Value("${app.auth.permitted-paths:}") List<String> permittedPaths) {
        // Create a mutable list and add default open endpoints
        this.permittedPaths = new ArrayList<>(permittedPaths);
        this.permittedPaths.addAll(List.of("/swagger/**", "/swagger-ui/**", "/v3/api-docs/**", "/graphiql/**", "/graphql/**"));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Skip authorization check for excluded/permitted paths
        for (String permittedPath : permittedPaths) {
            if (pathMatcher.match(permittedPath, path)) {
                return chain.filter(exchange);
            }
        }

        // 2. Get the handler mapped to this request
        Object handler = exchange.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

        if (handler instanceof HandlerMethod handlerMethod) {
            Authorize authorize = handlerMethod.getMethodAnnotation(Authorize.class);
            if (authorize == null) {
                authorize = handlerMethod.getBeanType().getAnnotation(Authorize.class);
            }

            // Return early if no @Authorize annotation is present
            if (authorize == null) {
                return chain.filter(exchange);
            }

            // Check permissions
            Set<String> tokenPermissions = exchange.getAttribute("permissions");
            if (tokenPermissions == null || tokenPermissions.isEmpty()) {
                log.warn("Access Denied: No permissions found in request context for path {}", path);
                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied"));
            }

            Set<String> requiredPermissions = Arrays.stream(authorize.permissions())
                    .collect(Collectors.toSet());

            boolean isAuthorized = requiredPermissions.stream().anyMatch(tokenPermissions::contains);

            if (!isAuthorized) {
                log.warn("Access Denied for path {}: Required permissions {}", path, requiredPermissions);
                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied"));
            }

            log.info("Access Granted for request: {}", path);
        }

        return chain.filter(exchange);
    }
}