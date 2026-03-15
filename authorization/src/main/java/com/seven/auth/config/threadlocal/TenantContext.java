package com.seven.auth.config.threadlocal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TenantContext {
    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_DB_VENDOR = new ThreadLocal<>();

    private TenantContext() {
    }

    public static String getCurrentTenant(){
        return Optional.ofNullable(CURRENT_TENANT.get()).orElse("public");
    }

    public static void setCurrentTenant(String schemaName){
        log.info("TenantContext: setting current tenant schema {}", schemaName);
        CURRENT_TENANT.set(Optional.ofNullable(schemaName).orElse("public"));
    }

    public static void clearTenant(){CURRENT_TENANT.remove();}

    public static String getCurrentDbVendor() {
        return Optional.ofNullable(CURRENT_DB_VENDOR.get()).orElse("postgresql");
    }

    public static void setCurrentDbVendor(String currentDbVendor) {
        CURRENT_DB_VENDOR.set(currentDbVendor);
    }

    public static void clearCurrentDbVendor(){CURRENT_DB_VENDOR.remove();}
}
