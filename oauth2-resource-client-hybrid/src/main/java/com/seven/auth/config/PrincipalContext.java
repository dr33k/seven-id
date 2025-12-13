package com.seven.auth.config;

import java.util.Optional;

public class PrincipalContext {
    public static ThreadLocal<String> CURRENT_PRINCIPAL = new ThreadLocal<>();

    public static String getCurrentPrincipal() {
        return Optional.ofNullable(CURRENT_PRINCIPAL.get()).orElse("");
    }

    public static void setCurrentPrincipal(String currentPrincipal) {
        CURRENT_PRINCIPAL.set(Optional.ofNullable(currentPrincipal).orElse(""));
    }

    public static void clearCurrentPrincipal(){CURRENT_PRINCIPAL.remove();}
}
