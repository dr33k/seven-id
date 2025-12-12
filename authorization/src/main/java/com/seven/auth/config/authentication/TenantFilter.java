package com.seven.auth.config.authentication;

import com.seven.auth.application.ApplicationRepository;
import com.seven.auth.config.threadlocal.TenantContext;
import com.seven.auth.exception.ConflictException;
import com.seven.auth.util.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    //These URIs do not require a tenant id
    private final List<String> whitelist = List.of("/swagger", "/swagger-ui", "/v3/api-docs", Constants.PATH_PREFIX + "/applications");
    private final ApplicationRepository applicationRepository;

    public TenantFilter(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            log.info("---------------------------------------AUTHORIZATION---------------------------------------------------------------");
            setTenant(request, (String) request.getAttribute("tenant"));
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clearTenant();
        }
    }

    private void setTenant(HttpServletRequest request, String tenant) {
        try {
            String path = request.getRequestURI();
            log.info("PATH: {}", path);
            //Paths not visited by users
            if (isPathWhitelisted(path)) {
                log.info("WHITELISTED: {}", path);
                TenantContext.setCurrentTenant(Constants.PUBLIC_SCHEMA);
            }

            //Authentication paths
            else if (isSuperuserAuthenticationPath(path)) {
                log.info("Superuser authentication path");
                TenantContext.setCurrentTenant(Constants.PUBLIC_SCHEMA);
            } else if (isRegularAuthenticationPath(path)) {
                log.info("Authentication path");
                String tenantId = request.getHeader("X-Tenant-Id");
                assert tenantId != null : "Tenant not provided";
                tenant = applicationRepository.findById(UUID.fromString(tenantId)).orElseThrow(() -> new ConflictException("Tenant with id %s not found".formatted(tenantId))).getSchemaName();
                TenantContext.setCurrentTenant(tenant);
            }

            //Requests performed by superusers
            else if (currentPrincipalIsSuperUser(tenant)) {
                log.info("Current principal is a superuser");
                String tenantId = request.getHeader("X-Tenant-Id");
                if (isRequestToCreateRegularAccount(request, path)) { //
                    assert tenantId != null : "Tenant not provided";
                    tenant = applicationRepository.findById(UUID.fromString(tenantId)).orElseThrow(() -> new ConflictException("Tenant with id %s not found".formatted(tenantId))).getSchemaName();
                }

                //If it is any other type of request performed by superusers
                else if (tenantId != null) {
                    tenant = applicationRepository.findById(UUID.fromString(tenantId)).orElseThrow(() -> new ConflictException("Tenant with id %s not found".formatted(tenantId))).getSchemaName();
                }

                TenantContext.setCurrentTenant(tenant);
            }

            //Requests performed by other users
            else {
                log.info("Current principal is a regular user");
                assert tenant != null : "Tenant not provided";
                TenantContext.setCurrentTenant(tenant);
            }
            log.info("Routed to tenant: {}", TenantContext.getCurrentTenant());
        } catch (Exception e) {
            String msg = "Error routing to tenant %s : %s".formatted(tenant, e.getMessage());
            log.error(msg);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
        }
    }

    private static boolean isRequestToCreateRegularAccount(HttpServletRequest request, String path) {
        return "POST".equals(request.getMethod()) && path.equals(Constants.PATH_PREFIX + "/accounts");
    }

    private static boolean currentPrincipalIsSuperUser(String tenant) {
        return Constants.PUBLIC_SCHEMA.equals(tenant);
    }

    private static boolean isRegularAuthenticationPath(String path) {
        return path.startsWith("/auth/");
    }

    private static boolean isSuperuserAuthenticationPath(String path) {
        return path.startsWith("/su/auth/");
    }

    private boolean isPathWhitelisted(String path) {
        return whitelist.stream().anyMatch(path::startsWith);
    }
}
