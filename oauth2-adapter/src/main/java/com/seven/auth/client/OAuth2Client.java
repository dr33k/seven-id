package com.seven.auth.client;

import com.seven.auth.dto.account.IAccount;
import com.seven.auth.dto.response.Res;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "oauth2-client", url = "${auth.base-url}/oauth2/authorization")
public interface OAuth2Client {

    @GetMapping(value = "/google", produces = MediaType.APPLICATION_JSON_VALUE)
    Res<IAccount.Record> googlelogin(@RequestParam(value = "tenant_id") UUID tenantId);

    @GetMapping(value = "/apple", produces = MediaType.APPLICATION_JSON_VALUE)
    Res<IAccount.Record> appleLogin(@RequestParam(value = "tenant_id") UUID tenantId);

}
