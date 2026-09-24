package com.seven.auth.client.authorization;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class AuthorizationHandlerInterceptorConfiguration implements WebMvcConfigurer {
    private final AuthorizationHandlerInterceptor authorizationHandlerInterceptor;

    private final List<String> permittedPaths;

    public AuthorizationHandlerInterceptorConfiguration(@Value("authorization.jwt.permitted-paths")List<String> permittedPaths, AuthorizationHandlerInterceptor authorizationHandlerInterceptor) {
        permittedPaths.addAll(List.of("/swagger/**", "/swagger-ui/**", "/v3/api-docs/**"));
        this.permittedPaths = permittedPaths;
        this.authorizationHandlerInterceptor = authorizationHandlerInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationHandlerInterceptor)
                .excludePathPatterns(permittedPaths);
    }
}
