package com.seven.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seven.auth.account.AccountDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final ObjectMapper objectMapper;

    public JwtAuthenticationConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Extract permissions from JWT claims
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        Collection<GrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Map the principal claim to your Record
        AccountDTO.Record accountRecord = objectMapper.convertValue(jwt.getClaim("principal"), AccountDTO.Record.class);

        return new AccountAuthenticationToken(accountRecord, authorities);
    }
}