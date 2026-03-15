package com.seven.auth.config;

import com.seven.auth.exception.ConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class TenantAwareOAuth2RequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareOAuth2RequestResolver.class);
    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public TenantAwareOAuth2RequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        // This is where you get the defaultResolver
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request);
        return customize(authRequest, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customize(authRequest, request);
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest authRequest, HttpServletRequest request) {
        if (authRequest == null) return null;

        // 1. Grab the tenantId from a query param (e.g., /oauth2/authorization/google?tenantId=yessah)
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId == null || tenantId.isEmpty()) {
            log.error("No tenantId provided");
            throw new ConflictException("No tenantId provided");
        }

        // 2. Append the tenantId to the state so Google returns it to us
        String newState = authRequest.getState() + ":" + tenantId;

        return OAuth2AuthorizationRequest.from(authRequest)
                .state(newState)
                .build();
    }
}