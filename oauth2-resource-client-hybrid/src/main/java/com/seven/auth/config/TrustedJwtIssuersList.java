package com.seven.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver.jwt")
public class TrustedJwtIssuersList {
    private List<String> trustedIssuers;
    public List<String> getTrustedIssuers() {
        return trustedIssuers;
    }
    public void setTrustedIssuers(List<String> trustedIssuers) {
        this.trustedIssuers = trustedIssuers;
    }
}
