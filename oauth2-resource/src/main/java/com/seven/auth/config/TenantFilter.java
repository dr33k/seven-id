package com.seven.auth.config;

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

            // Set Tenant and Attributes here
            String tenantId = request.getHeader("X-Tenant-Id");
            setTenant(request, tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clearTenant();
        }
    }

    private void setTenant(HttpServletRequest request, String tenantId) {
        try {
            String path = request.getRequestURI();
            log.info("PATH: {}", path);


            //Paths not visited by users
            if (isPathWhitelisted(path)) {
                log.info("* WHITELISTED: {} *", path);
                TenantContext.setCurrentTenant(Constants.PUBLIC_SCHEMA);
            }

            //Superuser authentication paths
            else if (isSuperuserAuthenticationPath(path)) {
                log.info("* Superuser authentication path *");
                TenantContext.setCurrentTenant(Constants.PUBLIC_SCHEMA);
            }

            else {
                assert tenantId != null : "Tenant not provided";
                String tenant = applicationRepository.findById(UUID.fromString(tenantId)).orElseThrow(() -> new ConflictException("Tenant with id %s not found".formatted(tenantId))).getSchemaName();

                //Regular user authentication path
                if (isRegularAuthenticationPath(path)) {
                    log.info("* Regular Authentication path *");
                }

                //Requests performed by other users
                else {
                    log.info("* Current principal is a regular user *");
                }

                TenantContext.setCurrentTenant(tenant);
            }
            log.info("Routed to tenant with id: {}", TenantContext.getCurrentTenant());
        } catch (Exception | AssertionError e) {
            String msg = "Error routing to tenant %s : %s".formatted(tenantId, e.getMessage());
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
