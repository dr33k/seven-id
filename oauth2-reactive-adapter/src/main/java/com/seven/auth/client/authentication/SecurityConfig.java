package com.seven.auth.client.authentication;

import com.seven.auth.client.authorization.AuthorizationFilter;
import com.seven.auth.config.JwtAuthenticationConverter;
import com.seven.auth.config.ReactiveJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;


@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Value("${app.auth.permitted-paths}")
    private List<String> permittedPaths;
    @Value("${app.auth.jwt.secret}")
    private String appJwtSecret;

    private final AuthorizationFilter authorizationFilter;
    private final ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter;

    public SecurityConfig(AuthorizationFilter authorizationFilter, ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter) {
        this.authorizationFilter = authorizationFilter;
        this.reactiveJwtAuthenticationConverter = reactiveJwtAuthenticationConverter;
    }

    @Bean
    @Primary
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        String[] paths = permittedPaths.toArray(new String[]{});
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        jwt -> jwt.jwtAuthenticationConverter(reactiveJwtAuthenticationConverter)
                        )
                )
                .addFilterAt(authorizationFilter, SecurityWebFiltersOrder.AUTHORIZATION)

                .authorizeExchange(exchanges -> {
                            if (paths.length > 0) {
                                exchanges.pathMatchers(paths).permitAll();
                            }
                            exchanges
                                    .pathMatchers("/swagger", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                    .anyExchange().authenticated();
                        }
                );
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // Assuming you use a HMAC Secret Key (HS512)
        byte[] secretKeyBytes = appJwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, "HmacSHA512");

        return NimbusReactiveJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

}