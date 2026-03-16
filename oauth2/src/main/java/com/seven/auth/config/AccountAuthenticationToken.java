package com.seven.auth.config;

import com.seven.auth.account.AccountDTO;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class AccountAuthenticationToken extends AbstractAuthenticationToken {
    private final AccountDTO.Record principal;
    private final String tenant;

    public AccountAuthenticationToken(AccountDTO.Record principal, String tenant, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.tenant = tenant;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() { return null; }

    @Override
    public Object getPrincipal() { return this.principal; }

    public String getTenant(){return tenant;}
}