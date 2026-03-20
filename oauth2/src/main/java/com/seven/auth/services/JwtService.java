package com.seven.auth.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seven.auth.account.Account;
import com.seven.auth.account.AccountDTO;
import com.seven.auth.account.AccountService;
import com.seven.auth.account.AuthDTO;
import com.seven.auth.config.threadlocal.TenantContext;
import com.seven.auth.dto.request.BearerTokenLoginRequest;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.exception.ClientException;
import com.seven.auth.exception.UnauthorizedException;
import com.seven.auth.permission.Permission;
import com.seven.auth.permission.PermissionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    final private AccountService accountService;
    final private PermissionRepository permissionRepository;
    private final AuthenticationProvider authenticationProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.jwt.issuer}")
    private String appJwtIssuer;
    @Value("${app.jwt.secret}")
    private String appJwtSecret;
    @Value("${app.jwt.expiration-hrs}")
    private String appJwtExp;

    public JwtService(AccountService accountService, PermissionRepository permissionRepository, BCryptPasswordEncoder bCryptPasswordEncoder, ObjectMapper objectMapper) {
        this.accountService = accountService;
        this.permissionRepository = permissionRepository;

        DaoAuthenticationProvider dao = new DaoAuthenticationProvider(accountService);
        dao.setUserDetailsPasswordService(accountService);
        dao.setPasswordEncoder(bCryptPasswordEncoder);
        this.authenticationProvider = dao;

        this.objectMapper = objectMapper;
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .decryptWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Key getSigningKey() {
        byte[] bytes = appJwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        // 1. Create a clean copy
        Map<String, Object> sanitizedClaims = new HashMap<>(claims);

        // 2. REMOVE standard claims that Google provides but JJWT wants to handle itself
        // If these stay in the map, JJWT will try to validate their types and fail.
        sanitizedClaims.remove("exp");
        sanitizedClaims.remove("iat");
        sanitizedClaims.remove("iss");
        sanitizedClaims.remove("sub");
        sanitizedClaims.remove("at_hash"); // Google specific, usually not needed in your app token

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(Long.parseLong(appJwtExp) * 3600);

        return Jwts.builder()
                .json(new io.jsonwebtoken.jackson.io.JacksonSerializer<>(objectMapper))
                .claims(sanitizedClaims) // Now it only contains custom data (email, name, etc.)
                .subject(subject)        // JJWT sets 'sub' correctly here
                .issuer(appJwtIssuer)    // JJWT sets 'iss' correctly here
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public AuthDTO register(AccountDTO.Create request) throws AuthorizationException {
        try {
            AccountDTO.Record accountRecord = accountService.create(request);
            List<String> permissions = permissionRepository.findAllByAccount(accountRecord.email()).stream().map(Permission::getName).toList();

            String token = generateToken(accountRecord.email(),
                    Map.of("permissions", permissions,
                            "principal", accountRecord,
                            "tenant", TenantContext.getCurrentTenant())
            );
            return AuthDTO.builder().data(accountRecord).token(token).build();
        } catch (AuthorizationException e) {
            log.error("ResponseStatusException; Unable to register account {}. Message: ", request.email(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unable to register account {}. Message: ", request.email(), e);
            throw new ClientException(e.getMessage());
        }
    }

    public AuthDTO registerSuper(AccountDTO.Create request) throws AuthorizationException {
        try {
            AccountDTO.Record accountRecord = accountService.createSuper(request);
            List<String> permissions = permissionRepository.findAllByAccount(accountRecord.email()).stream().map(Permission::getName).toList();

            String token = generateToken(accountRecord.email(),
                    Map.of("permissions", permissions,
                            "principal", accountRecord,
                            "tenant", TenantContext.getCurrentTenant())
            );
            return AuthDTO.builder().data(accountRecord).token(token).build();
        } catch (AuthorizationException e) {
            log.error("ResponseStatusException; Unable to register superuser {}. Message: ", request.email(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unable to register superuser {}. Message: ", request.email(), e);
            throw new ClientException(e.getMessage());
        }
    }

    @Transactional
    public AuthDTO login(BearerTokenLoginRequest request) throws AuthorizationException {
        try {
            String tenant = TenantContext.getCurrentTenant();
            log.info("Login username: {}; tenant: {}", request.getUsername(), tenant);
            Account account = (Account) authenticationProvider
                    .authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()))
                    .getPrincipal();
            AccountDTO.Record accountRecord = AccountDTO.Record.from(account);
            List<String> permissions = permissionRepository.findAllByAccount(accountRecord.email()).stream().map(Permission::getName).toList();

            String token = generateToken(account.getEmail(),
                    Map.of("permissions", permissions,
                            "principal", accountRecord,
                            "tenant", tenant)
            );
            log.info("User {} logged in successfully", request.getUsername());
            return AuthDTO.builder().data(accountRecord).token(token).build();
        } catch (Exception e) {
            log.error("Unable to login {}. Message: ", request.getUsername(), e);
            throw new UnauthorizedException(e.getMessage());
        }
    }
}
