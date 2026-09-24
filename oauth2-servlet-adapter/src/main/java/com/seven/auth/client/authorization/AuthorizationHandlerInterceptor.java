package com.seven.auth.client.authorization;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthorizationHandlerInterceptor implements HandlerInterceptor {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        try {
            Set<String> tokenPermissions = ((Set<String>) request.getAttribute("permissions"));
            log.info("User permissions: {}", tokenPermissions);

            if (handler instanceof HandlerMethod) {
                Method method = ((HandlerMethod) handler).getMethod();
                Authorize authorize = method.getAnnotation(Authorize.class);

                if (authorize == null) {
                    log.info("UNAUTHORIZED ENDPOINT");
                    return true;
                }
                Set<String> annPermissions = Arrays.stream(authorize.permissions()).collect(Collectors.toSet());

                boolean isAuthorized = annPermissions.stream().anyMatch(tokenPermissions::contains);
                if (!isAuthorized) {
                    log.warn("UNAUTHORIZED USER");
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
                }
                log.info("AUTHORIZED");
                return true;
            }
            else if (handler instanceof ResourceHttpRequestHandler) return true;
            else {
                log.warn("HANDLER INTERCEPTOR LEAK");
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
            }
        } catch (Exception e) {
            log.error("Error in Authorization Handler Interceptor: ", e);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied");
        }
    }
}
