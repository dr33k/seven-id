package com.seven.auth.config;

import com.seven.auth.application.ApplicationRepository;
import com.seven.auth.config.threadlocal.TenantContext;
import com.seven.auth.exception.ConflictException;
import com.seven.auth.exception.ForbiddenException;
import com.seven.auth.util.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/*
This filter handles the marshalling of requests to the DB tenant the authenticated user wishes to visit, not the tenant they belong to
 */
@Component
public class TenantFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //These URIs do not require a tenant id
    private final List<String> whitelist = List.of(
            "/swagger", "/swagger-ui", "/v3/api-docs", Constants.PATH_PREFIX + "/applications",
            "/.well-known/appspecific/com.chrome.devtools.json", "/favicon.ico");
    private final ApplicationRepository applicationRepository;

    public TenantFilter(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    private static boolean isRequestToCreateRegularAccount(HttpServletRequest request, String path) {
        return "POST".equals(request.getMethod()) && path.equals(Constants.PATH_PREFIX + "/accounts");
    }

    private static boolean isRequestFromSuperuser(String accountTenant) {
        return Constants.PUBLIC_SCHEMA.equals(accountTenant);
    }

    private static boolean isRegularAuthRequest(String path) {
        return path.startsWith("/auth/");
    }

    private static boolean isSuperuserAuthRequest(String path) {
        return path.startsWith("/su/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String destinationTenantId = request.getHeader("X-Tenant-Id");
        try {
            log.info("---------------------------------------TENANT-FILTER---------------------------------------------------------------");

            String accountTenant = (String) request.getAttribute("tenant");
            setDestinationTenant(request, accountTenant, destinationTenantId);
            filterChain.doFilter(request, response);
        } catch (Exception | AssertionError e) {
            String msg = "Error routing to tenant %s : %s".formatted(destinationTenantId, e.getMessage());
            log.error(msg);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"" + e.getMessage() + "\"}");
        } finally {
            TenantContext.clearTenant();
        }
    }

    private void setDestinationTenant(HttpServletRequest request, String accountTenant, String destinationTenantId) {
        String path = request.getRequestURI();
        log.info("PATH: {}", path);


        //Paths not visited by users
        if (isPathWhitelisted(path)) {
            log.info("* WHITELISTED: {} *", path);
            TenantContext.setCurrentTenant(Constants.PUBLIC_SCHEMA);
        } else if (isSuperuserAuthRequest(path)) {
            log.info("* Superuser authentication request *");
            TenantContext.setCurrentTenant(Constants.PUBLIC_SCHEMA);
        } else if (!isRegularAuthRequest(path) && !isRequestFromSuperuser(accountTenant)) {
            log.info("* Regular User request *");
            TenantContext.setCurrentTenant(accountTenant);
        } else { //Destination Tenant id required
            if (destinationTenantId == null) throw new ForbiddenException("Destination Tenant not provided");
            String destinationTenant = applicationRepository.findById(UUID.fromString(destinationTenantId)).orElseThrow(() -> new ConflictException("Destination Tenant with id %s not found".formatted(destinationTenantId))).getSchemaName();

            if (isRegularAuthRequest(path)) {
                log.info("* Regular Authentication request *");
                TenantContext.setCurrentTenant(destinationTenant);
            } else if (isRequestFromSuperuser(path)) {
                log.info("* Superuser request *");
                TenantContext.setCurrentTenant(destinationTenant);
            }
        }

        log.info("Routed to tenant: {}", TenantContext.getCurrentTenant());
    }

    private boolean isPathWhitelisted(String path) {
        return whitelist.stream().anyMatch(path::startsWith);
    }
}
