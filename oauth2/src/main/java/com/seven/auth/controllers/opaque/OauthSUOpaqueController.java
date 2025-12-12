package com.seven.auth.controllers.opaque;

import com.seven.auth.OauthService;
import com.seven.auth.account.AccountDTO;
import com.seven.auth.dto.jwt.JwtLoginRequest;
import com.seven.auth.dto.response.Response;
import com.seven.auth.exception.AuthorizationException;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("su/auth/oauth2/opaque")
public class OauthSUOpaqueController {
    private final OauthService oauth2Service;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public OauthSUOpaqueController(OauthService oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    @SecurityRequirements
    @PostMapping(value = "/{provider}/login", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Response> login(
            @PathVariable("provider") String provider,
            @Valid @RequestBody JwtLoginRequest request) throws AuthorizationException {
        log.info("OAuth2 Opaque variant provider {}", provider);
        return null;
    }

    @PostMapping(value = "/{variant}/{provider}/provision", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Response> provisionSuper(
            @PathVariable("provider") String provider,
            @Valid @RequestBody AccountDTO.Create request) throws AuthorizationException {
        log.info("OAuth2 Opaque variant provider {}", provider);
        return null;
    }
}
