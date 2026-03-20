package com.seven.auth.controllers;

import com.seven.auth.account.AccountDTO;
import com.seven.auth.account.AuthDTO;
import com.seven.auth.dto.request.BearerTokenLoginRequest;
import com.seven.auth.dto.response.Res;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.services.JwtService;
import com.seven.auth.util.Constants;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.seven.auth.dto.response.Responder.*;

@RestController
@RequestMapping("/auth")
@SecurityRequirements
public class JwtAuthController {
    JwtService jwtService;

    public JwtAuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping(value = "/register", produces = "application/json", consumes = "application/json")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER, required = true)
    public ResponseEntity<Res> createResource(@Valid @RequestBody AccountDTO.Create request) throws AuthorizationException {
            AuthDTO userDTO = jwtService.register(request);
            return created(userDTO.data, userDTO.token, "/domains" );
    }

    @PostMapping(value = "/login", produces = "application/json", consumes = "application/json")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER, required = true)
    public ResponseEntity<Res> login(@Valid @RequestBody BearerTokenLoginRequest request) throws AuthorizationException {
        AuthDTO userDTO = jwtService.login(request);
        return ok(userDTO.data, userDTO.token);
    }

    @GetMapping(value = "/oauth2/login-success", produces = "application/json", consumes = "*/*")
    public ResponseEntity<Res> oauth2LoginSuccess(@CookieValue("X-Seven-Jwt")Cookie cookie) throws AuthorizationException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(cookie.getName(), cookie.getValue());
        return new ResponseEntity<>(headers, HttpStatusCode.valueOf(204));
    }
}
