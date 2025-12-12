package com.seven.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver.opaquetoken")
public class IntrospectionServerList {
    private List<IntrospectionServer> servers;
    public IntrospectionServerList() {
    }
    public List<IntrospectionServer> getServers() {
        return servers;
    }
    public void setServers(List<IntrospectionServer> servers) {
        this.servers = servers;
    }
}

class IntrospectionServer {
    private String name;
    private String introspectionUri;
    private String clientId;
    private String clientSecret;

    public IntrospectionServer() {
    }

    public String getIntrospectionUri() {
        return introspectionUri;
    }

    public void setIntrospectionUri(String introspectionUri) {
        this.introspectionUri = introspectionUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
