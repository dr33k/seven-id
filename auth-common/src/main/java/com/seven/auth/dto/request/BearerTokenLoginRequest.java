package com.seven.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public class BearerTokenLoginRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;

    public BearerTokenLoginRequest() {
    }

    public @NotBlank String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank String username) {
        this.username = username;
    }

    public @NotBlank String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank String password) {
        this.password = password;
    }
}
