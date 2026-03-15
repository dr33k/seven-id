package com.seven.auth.client;

import com.seven.auth.dto.account.IAccount;
import com.seven.auth.dto.jwt.JwtLoginRequest;
import com.seven.auth.dto.response.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "jwt-client", url = "${authentication.jwt.base-url}/auth/jwt")
public interface JwtClient {

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<IAccount.Record> register(@RequestHeader(value = "X-Tenant-Id") UUID tenantId,
                                       @RequestBody IAccount.Request request);

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<IAccount> login(@RequestHeader(value = "X-Tenant-Id") UUID tenantId,
                                      @RequestBody JwtLoginRequest request);
}
