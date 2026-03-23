package com.seven.auth.controllers;

import com.seven.auth.account.AccountDTO;
import com.seven.auth.account.AuthDTO;
import com.seven.auth.request.BearerTokenLoginRequest;
import com.seven.auth.response.Res;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.services.JwtService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.seven.auth.response.Responder.ok;

@RestController
@RequestMapping("su/auth")
public class JwtSUAuthController {
    private final JwtService jwtService;

    public JwtSUAuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @SecurityRequirements()
    @PostMapping(value = "/login", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Res> login(@Valid @RequestBody BearerTokenLoginRequest request) throws AuthorizationException {
        AuthDTO userDTO = jwtService.login(request);
        return ok(userDTO.data, userDTO.token);
    }

    @PostMapping(value = "/provision", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Res> provisionSuper(@Valid @RequestBody AccountDTO.Create request) throws AuthorizationException {
        AuthDTO userDTO = jwtService.registerSuper(request);
        return ok(userDTO.data, userDTO.token);
    }
}
