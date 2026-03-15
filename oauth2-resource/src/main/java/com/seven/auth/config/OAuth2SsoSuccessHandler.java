package com.seven.auth.config;

import com.seven.auth.account.Account;
import com.seven.auth.account.AccountDTO;
import com.seven.auth.account.AccountRepository;
import com.seven.auth.account.AuthProvider;
import com.seven.auth.config.threadlocal.TenantContext;
import com.seven.auth.exception.ConflictException;
import com.seven.auth.permission.Permission;
import com.seven.auth.permission.PermissionRepository;
import com.seven.auth.services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2SsoSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    private final PermissionRepository permissionRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public OAuth2SsoSuccessHandler(JwtService jwtService, AccountRepository accountRepository, PermissionRepository permissionRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
        this.permissionRepository = permissionRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId(); // "google", "github", etc.

        // 1. Map OIDC claims to your internal AccountDTO.Record
        String email = oidcUser.getEmail();

        accountRepository.findByEmail(email).ifPresent(
                account -> {
                    log.error("Account with email {} already exists", email);
                    throw new ConflictException("Account with email %s already exists".formatted(email));
                });

        Account a = accountRepository.save(
                new Account(
                        AuthProvider.valueOf(provider),
                        oidcUser.getGivenName(),
                        oidcUser.getFamilyName(),
                        oidcUser.getPhoneNumber(),
                        email,
                        bCryptPasswordEncoder.encode(UUID.randomUUID().toString())
                )
        );

        AccountDTO.Record accountRecord = AccountDTO.Record.from(a);
        List<String> permissions = permissionRepository.findAllByAccount(accountRecord.email()).stream().map(Permission::getName).toList();

        Map<String, Object> attributes = oidcUser.getAttributes();
        attributes.put("principal", accountRecord);
        attributes.put("permissions", permissions);
        attributes.put("tenant", TenantContext.getCurrentTenant());


        // 2. Generate your Custom Bearer Token
        String token = jwtService.generateToken(email, attributes);

        // 3. Redirect to your Frontend with the token in a query param
        // (In production, consider a secure cookie or a specialized redirect)
        String targetUrl = "http://localhost:8080/login-success?token=" + token;

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}