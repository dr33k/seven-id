package com.seven.auth.client;

import com.seven.auth.account.AccountDTO;
import com.seven.auth.request.BearerTokenLoginRequest;
import com.seven.auth.response.Res;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "jwt-client", url = "${auth.base-url}/auth")
public interface JwtClient {

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    Res<AccountDTO.Record> register(@RequestHeader(value = "X-Tenant-Id") UUID tenantId,
                                    @RequestBody AccountDTO.Create request);

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    Res<AccountDTO> login(@RequestHeader(value = "X-Tenant-Id") UUID tenantId,
                        @RequestBody BearerTokenLoginRequest request);
}
