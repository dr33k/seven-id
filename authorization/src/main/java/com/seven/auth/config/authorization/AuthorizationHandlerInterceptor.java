package com.seven.auth.config.authorization;

import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.exception.ForbiddenException;
import com.seven.auth.permission.PEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AuthorizationHandlerInterceptor implements HandlerInterceptor {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws AuthorizationException{
        try {
            List<String> tokenPermissions = Optional.ofNullable((List<String>)request.getAttribute("permissions")).orElse(Collections.EMPTY_LIST);
            log.info("User permissions: {}", tokenPermissions);

            if (handler instanceof HandlerMethod) {
                Method method = ((HandlerMethod) handler).getMethod();
                Authorize authorize = method.getAnnotation(Authorize.class);

                if (authorize == null) {
                    log.info("UNAUTHORIZED ENDPOINT");
                    return true;
                }
                Set<String> annPermissions = Arrays.stream(authorize.permissions()).map(PEnum::name).collect(Collectors.toSet());

                boolean isAuthorized = annPermissions.stream().anyMatch(tokenPermissions::contains);
                if (!isAuthorized) {
                    log.warn("UNAUTHORIZED USER");
                    throw new ForbiddenException("Access Denied");
                }
                log.info("AUTHORIZED");
                return true;
            }
            else if (handler instanceof ResourceHttpRequestHandler) return true;
            else {
                log.warn("HANDLER INTERCEPTOR LEAK");
                throw new ForbiddenException("Access Denied");
            }
        } catch (AuthorizationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in Authorization Handler Interceptor: ", e);
            throw new ForbiddenException("Access Denied");
        }
    }
}
