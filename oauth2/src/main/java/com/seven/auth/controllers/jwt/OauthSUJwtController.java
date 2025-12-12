package com.seven.auth.controllers.jwt;

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

import static com.seven.auth.dto.response.Responder.ok;

@RestController
@RequestMapping("su/auth/oauth2/jwt")
public class OauthSUJwtController {
    private final OauthService oauth2Service;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public OauthSUJwtController(OauthService oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    @SecurityRequirements
    @PostMapping(value = "/{provider}/login", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Response> login(
            @PathVariable("provider") String provider,
            @Valid @RequestBody JwtLoginRequest request) throws AuthorizationException {
        log.info("OAuth2 JWT variant provider {}", provider);
//        AuthDTO userDTO = oauth2Service.login(request);
//        return ok(userDTO.data, userDTO.token);
        return null;
    }

    @PostMapping(value = "/{provider}/provision", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Response> provisionSuper(
            @PathVariable("provider") String provider,
            @Valid @RequestBody AccountDTO.Create request) throws AuthorizationException {
        log.info("OAuth2 JWT variant provider {}", provider);

//        AuthDTO userDTO = oauth2Service.registerSuper(request);
//        return ok(userDTO.data, userDTO.token);
        return null;
    }
}
