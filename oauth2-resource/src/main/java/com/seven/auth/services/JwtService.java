package com.seven.auth.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seven.auth.account.Account;
import com.seven.auth.account.AccountDTO;
import com.seven.auth.account.AccountService;
import com.seven.auth.account.AuthDTO;
import com.seven.auth.config.threadlocal.TenantContext;
import com.seven.auth.dto.jwt.JwtLoginRequest;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.exception.ClientException;
import com.seven.auth.permission.Permission;
import com.seven.auth.permission.PermissionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.annotation.ApplicationScope;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@ApplicationScope
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final Environment env;
    final private AccountService accountService;
    final private PermissionRepository permissionRepository;
    private final AuthenticationProvider authenticationProvider;
    private final ObjectMapper objectMapper;

    public JwtService(Environment env, AccountService accountService, PermissionRepository permissionRepository, AuthenticationProvider authenticationProvider, ObjectMapper objectMapper) {
        this.env = env;
        this.accountService = accountService;
        this.permissionRepository = permissionRepository;
        this.authenticationProvider = authenticationProvider;
        this.objectMapper = objectMapper;
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] bytes = Objects.requireNonNull(env.getProperty("app.jwt.secret")).getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        ZonedDateTime now = ZonedDateTime.now();
        return Jwts
                .builder()
                .serializeToJsonWith(new JacksonSerializer <>(objectMapper))
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(now.plusHours(
                        Integer.parseInt(Objects.requireNonNull(env.getProperty("app.jwt.secret")))).toInstant())
                )
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
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
    public AuthDTO login(JwtLoginRequest request) throws AuthorizationException{
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
            throw new ClientException(e.getMessage());
        }
    }
}
