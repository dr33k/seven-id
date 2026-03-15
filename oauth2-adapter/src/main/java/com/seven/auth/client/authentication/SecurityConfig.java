package com.seven.auth.client.authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private List<String> permittedPaths;
    private Environment env;

    public SecurityConfig(@Value("authentication.jwt.permitted-paths")List<String> permittedPaths) {
        this.permittedPaths = permittedPaths;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(permittedPaths.toArray(new String[]{})).permitAll()
                                .requestMatchers("/swagger", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                .anyRequest().authenticated()
                )

                .sessionManagement(httpSecuritySessionManagementConfigurer -> httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Assuming you use a HMAC Secret Key (HS256)
        byte[] secretKeyBytes = Objects.requireNonNull(env.getProperty("app.jwt.expiration-hrs")).getBytes(StandardCharsets.UTF_8);;
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, "HmacSHA512");

        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }


}