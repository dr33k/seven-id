package com.seven.auth.config;

import com.seven.auth.account.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final Environment env;
    private final TenantFilter tenantFilter;
    private final ClaimsExtractionFilter claimsExtractionFilter;
    private final OAuth2SsoSuccessHandler successHandler;


    private final Logger log = LoggerFactory.getLogger(getClass());

    public SecurityConfig(
            Environment env, TenantFilter tenantFilter, ClaimsExtractionFilter claimsExtractionFilter, OAuth2SsoSuccessHandler successHandler
    ) {
        this.env = env;
        this.tenantFilter = tenantFilter;
        this.claimsExtractionFilter = claimsExtractionFilter;
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter converter) throws Exception {
        http
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(HttpMethod.POST, "/auth/**", "/su/auth/*/*/login**").permitAll()
                                .requestMatchers("/swagger", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                .anyRequest().authenticated()
                )

                //Validate Oauth2 OIDC Token with OAuth2AuthenticationFilter
                .oauth2Login(oauth2 -> oauth2.successHandler(successHandler))

                //Handle API requests using BearerTokenAuthenticationFilter
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                )

                .addFilterBefore(tenantFilter, OAuth2LoginAuthenticationFilter.class)
                .addFilterAfter(claimsExtractionFilter, BearerTokenAuthenticationFilter.class)


                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Assuming you use a HMAC Secret Key (HS256)
        byte[] secretKeyBytes = Objects.requireNonNull(env.getProperty("app.jwt.expiration-hrs")).getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, "HmacSHA512");

        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(AccountService accountService, BCryptPasswordEncoder bCryptPasswordEncoder) {
        DaoAuthenticationProvider dao = new DaoAuthenticationProvider(accountService);
        dao.setUserDetailsPasswordService(accountService);
        dao.setPasswordEncoder(bCryptPasswordEncoder);
        return dao;
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}