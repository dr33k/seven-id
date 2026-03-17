package com.seven.auth.config;

import com.seven.auth.account.Account;
import com.seven.auth.account.AccountDTO;
import com.seven.auth.account.AccountRepository;
import com.seven.auth.account.AuthProvider;
import com.seven.auth.application.ApplicationRepository;
import com.seven.auth.config.threadlocal.TenantContext;
import com.seven.auth.exception.ConflictException;
import com.seven.auth.permission.Permission;
import com.seven.auth.permission.PermissionRepository;
import com.seven.auth.services.JwtService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OAuth2SsoSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final ApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final PermissionRepository permissionRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EntityManager em;
    private final TransactionTemplate transactionTemplate;
    private final String baseUrl;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public OAuth2SsoSuccessHandler(String baseUrl, JwtService jwtService, ApplicationRepository applicationRepository, AccountRepository accountRepository,
                                   PermissionRepository permissionRepository, BCryptPasswordEncoder bCryptPasswordEncoder,
                                   EntityManager em, TransactionTemplate transactionTemplate) {
        this.jwtService = jwtService;
        this.applicationRepository = applicationRepository;
        this.accountRepository = accountRepository;
        this.permissionRepository = permissionRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.em = em;
        this.transactionTemplate = transactionTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String token = transactionTemplate.execute(status -> {

            try {
                DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
                OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                String provider = oauthToken.getAuthorizedClientRegistrationId(); // "google", "github", etc.

                log.info("-------------------{}-OAUTH2-LOGIN-SUCCESS---------------------------", provider);
                // 1. Map OIDC claims to your internal AccountDTO.Record
                String email = oidcUser.getEmail();

                //Get tenant from state
                String state = request.getParameter("state");
                String tenant;
                if (state != null && state.contains(":")) {
                    String tenantId = state.split(":")[1];
                    tenant = applicationRepository.findById(UUID.fromString(tenantId)).orElseThrow(() -> new ConflictException("Tenant with id %s not found".formatted(tenantId))).getSchemaName();
                    TenantContext.setCurrentTenant(tenant);
                    log.info("Switching to schema for tenant: {}", tenant);
                } else {
                    log.error("Tenant id not found in 'state' property of OIDC callback");
                    throw new ConflictException("Tenant id not found in 'state' property of OIDC callback");
                }

                em.createNativeQuery("SET SCHEMA '%s'".formatted(tenant)).executeUpdate();

                Account a = accountRepository.findByEmail(email).orElseGet(() -> {
                    log.info("Account with email doesn't exist; Creating...");
                    return accountRepository.save(
                            new Account(
                                    AuthProvider.valueOf(provider),
                                    oidcUser.getGivenName(),
                                    oidcUser.getFamilyName(),
                                    oidcUser.getPhoneNumber(),
                                    email,
                                    bCryptPasswordEncoder.encode(UUID.randomUUID().toString())
                            )
                    );
                });

                AccountDTO.Record accountRecord = AccountDTO.Record.from(a);
                List<String> permissions = permissionRepository.findAllByAccount(accountRecord.email()).stream().map(Permission::getName).toList();

                Map<String, Object> attributes = new HashMap<>(oidcUser.getAttributes());
                attributes.put("principal", accountRecord);
                attributes.put("permissions", permissions);
                attributes.put("tenant", tenant);

                // 2. Generate your Custom Bearer Token
                String generatedToken = jwtService.generateToken(email, attributes);
                log.info("Bearer token generated for user {}", email);
                return generatedToken;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }

        });

        // 3. Redirect to your Frontend with the token in a query param
        Cookie jwtCookie = new Cookie("X-Seven-Jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(3600);
        response.addCookie(jwtCookie);

        String targetUrl = "/auth/oauth2/login-success";
        log.info("Redirecting to /auth/oauth2/login-success with a jwt-cookie");

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}