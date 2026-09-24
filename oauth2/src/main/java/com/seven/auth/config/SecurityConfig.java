package com.seven.auth.config;

import com.seven.auth.account.AccountRepository;
import com.seven.auth.application.ApplicationRepository;
import com.seven.auth.permission.PermissionRepository;
import com.seven.auth.services.JwtService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("${app.jwt.secret}")
    private String appJwtSecret;
    @Value("${app.jwt.issuer}")
    private String appJwtIssuer;

    private final JwtAuthenticationConverter converter;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final TenantFilter tenantFilter;
    private final AccountRepository accountRepository;
    private final PermissionRepository permissionRepository;
    private final ClaimsExtractionFilter claimsExtractionFilter;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ApplicationRepository applicationRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public SecurityConfig(JwtAuthenticationConverter converter, JwtService jwtService, BCryptPasswordEncoder bCryptPasswordEncoder, TenantFilter tenantFilter, AccountRepository accountRepository, PermissionRepository permissionRepository, ClaimsExtractionFilter claimsExtractionFilter, ClientRegistrationRepository clientRegistrationRepository, ApplicationRepository applicationRepository, EntityManager entityManager, TransactionTemplate transactionTemplate) {
        this.converter = converter;
        this.jwtService = jwtService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.tenantFilter = tenantFilter;
        this.accountRepository = accountRepository;
        this.permissionRepository = permissionRepository;
        this.claimsExtractionFilter = claimsExtractionFilter;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.applicationRepository = applicationRepository;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .securityMatcher("/api/**", "/auth/**", "/su/auth/**", "/login/**","/oauth2/**", "/v3/api-docs/**", "/swagger","/swagger-ui/**", "/.well-known/**")

                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers("/auth/**", "/su/auth/**").permitAll() // Whitelist local login and signup
                                .requestMatchers("/login/**", "/oauth2/**").permitAll() // Whitelist OIDC paths
                                .requestMatchers("/swagger", "/swagger-ui/**", "/v3/api-docs/**", "/.well-known/**").permitAll()
                                .anyRequest().authenticated()
                )

                //Validate Oauth2 OIDC Token with OAuth2AuthenticationFilter
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization ->
                                authorization.authorizationRequestResolver(new TenantAwareOAuth2RequestResolver(clientRegistrationRepository)))

                        .successHandler(oAuth2SsoSuccessHandler()))

                //Handle API requests using BearerTokenAuthenticationFilter
                .oauth2ResourceServer(oauth2 -> oauth2
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                )

                .addFilterAfter(claimsExtractionFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, ClaimsExtractionFilter.class)


                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }

    public OAuth2SsoSuccessHandler oAuth2SsoSuccessHandler() {
        return new OAuth2SsoSuccessHandler(appJwtIssuer, jwtService, applicationRepository, accountRepository, permissionRepository, bCryptPasswordEncoder, entityManager, transactionTemplate);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Assuming you use a HMAC Secret Key (HS256)
        byte[] secretKeyBytes = appJwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, "HmacSHA512");

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}