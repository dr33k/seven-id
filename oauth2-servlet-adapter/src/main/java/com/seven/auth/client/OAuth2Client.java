package com.seven.auth.client;

import com.seven.auth.account.AccountDTO;
import com.seven.auth.response.Res;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "oauth2-client", url = "${auth.base-url}/oauth2/authorization")
public interface OAuth2Client {

    @GetMapping(value = "/google", produces = MediaType.APPLICATION_JSON_VALUE)
    Res<AccountDTO.Record> googlelogin(@RequestParam(value = "tenant_id") UUID tenantId);

    @GetMapping(value = "/apple", produces = MediaType.APPLICATION_JSON_VALUE)
    Res<AccountDTO.Record> appleLogin(@RequestParam(value = "tenant_id") UUID tenantId);

}
