package com.seven.auth.config;


import com.seven.auth.account.AccountRepository;
import com.seven.auth.config.authentication.AuthVariant;
import com.seven.auth.config.threadlocal.TenantContext;
import com.seven.auth.permission.PermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class OAuthResourceServerConfiguration {
    private final List<String> trustedJwtIssuers;
    private final List<IntrospectionServer> trustedIntrospectionServers;
    private final AccountRepository accountRepository;
    private final PermissionRepository permissionRepository;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public OAuthResourceServerConfiguration(
            TrustedJwtIssuersList trustedJwtIssuersList,
            IntrospectionServerList introspectionServerList
    ) {
        this.trustedJwtIssuers = trustedJwtIssuersList.getTrustedIssuers();
        this.trustedIntrospectionServers = introspectionServerList.getServers();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(HttpMethod.POST, "/auth/**", "/su/auth/*/*/login**").permitAll()
                                .requestMatchers("/swagger", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                .anyRequest().authenticated()
                )

                .oauth2ResourceServer((oauth2) -> oauth2
                        //Validate oauth tokens using JWKS
                        .authenticationManagerResolver(this.tokenAuthenticationManagerResolver())
                        //Extract principal properties from token
                        .jwt((jwt)-> jwt.jwtAuthenticationConverter(this.jwtAuthenticationConverter()))
                )

                .oauth2Login(Customizer.withDefaults())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        ;
        return http.build();
    }

    //Configuration to switch between Opaque tokens and JWTs
    public AuthenticationManagerResolver<HttpServletRequest> tokenAuthenticationManagerResolver() {
        JwtIssuerAuthenticationManagerResolver jwt = JwtIssuerAuthenticationManagerResolver
                .fromTrustedIssuers(trustedJwtIssuers);

        Map<String, AuthenticationManager> otMap = getOpaqueTokenAuthManagerMap();

        return (request) -> {
            String uri = request.getRequestURI();
            if (uri.startsWith("/auth/oauth2/jwt")) return jwt.resolve(request);
            else if (uri.startsWith("/auth/oauth2/opaque")) return resolveOpaqueToken(request, otMap);
            else throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        };
    }

    private AuthenticationManager resolveOpaqueToken(HttpServletRequest request, Map<String, AuthenticationManager> otMap) throws ResponseStatusException {
        try {
            String introspectionServerName = request.getRequestURI().substring(1).split("/")[3];
            return otMap.get(introspectionServerName);
        } catch (Exception e) {
            log.error("Error resolving opaque token: ", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private Map<String, AuthenticationManager> getOpaqueTokenAuthManagerMap() {
        return trustedIntrospectionServers.stream()
                .collect(
                        Collectors.toMap(
                                IntrospectionServer::getName,
                                server ->
                                        new OpaqueTokenAuthenticationProvider(
                                                new SpringOpaqueTokenIntrospector(server.getIntrospectionUri(), server.getClientId(), server.getClientSecret())
                                        )::authenticate
                        )
                );
    }
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    public JwtAuthenticationConverter jwtAuthenticationConverter(){
        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter((jwt)-> {
            String email = jwt.getClaimAsString("email");
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");
            PrincipalContext.setCurrentPrincipal("%s:%s:%s".formatted(email, firstName, lastName));

            TenantContext.setCurrentAuthVariant(AuthVariant.OAUTH2);
            return List.of();
        });
        return jwtAuthenticationConverter;
    }
}