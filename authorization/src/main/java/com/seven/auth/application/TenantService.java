package com.seven.auth.application;

import com.seven.auth.account.Account;
import com.seven.auth.account.AccountRepository;
import com.seven.auth.config.threadlocal.TenantContext;
import com.seven.auth.domain.Domain;
import com.seven.auth.domain.DomainRepository;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.exception.ConflictException;
import com.seven.auth.util.Constants;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * The TenantService performs the singular task of registering an app and
 * populating a schema for it with tables using Flyway
 */
@Service
public class TenantService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final DomainRepository domainRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final DataSource dataSource;
    private final EntityManager em;
    private final TransactionTemplate transactionTemplate;

    public TenantService(ApplicationRepository applicationRepository, AccountRepository accountRepository, DomainRepository domainRepository, BCryptPasswordEncoder bCryptPasswordEncoder,
                         DataSource dataSource, EntityManager em, TransactionTemplate transactionTemplate) {
        this.applicationRepository = applicationRepository;
        this.accountRepository = accountRepository;
        this.domainRepository = domainRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.dataSource = dataSource;
        this.em = em;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public ApplicationDTO.Record register(ApplicationDTO.Create appRequest) throws AuthorizationException {
        try {
            String schemaName = appRequest.schemaName();
            log.info("Registering new app: {}", appRequest.name());

            //Check if app by this name already exists
            Application application = applicationRepository.findBySchemaName(schemaName).orElse(null);
            if (application == null) {
                application = provisionSchema(appRequest);
            }

            ApplicationDTO.Record appRecord = ApplicationDTO.Record.from(application);
            log.info("App: {} registered successfully with id: {}", appRecord.name(), appRecord.id());

            return appRecord;
        } catch (AuthorizationException e) {
            log.error("AuthorizationException registering app {}; Message: {}", appRequest.name(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error registering app {}; Trace:", appRequest.name(), e);
            throw new ConflictException(e.getMessage());
        }
    }

    private Application provisionSchema(ApplicationDTO.Create appRequest) throws AuthorizationException {
        try {
            log.info("Provisioning new schema for app: {}", appRequest.name());

            //Create flyway instance for schema-to-be-created
            Flyway tenantFlyway = buildTenantFlyway(dataSource, appRequest);

            transactionTemplate.execute(status -> {
                try {
                    //Clean up any incompletely provisioned schemas with the same name from the db
                    tenantFlyway.clean();

                    //Create and migrate new schema
                    tenantFlyway.migrate();
                    log.info("Schema: {} created successfully in DB", appRequest.schemaName());

                    return true;
                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw e;
                }
            });

            //Switch to newly created schema
            em.createNativeQuery("SET SCHEMA '%s'".formatted(appRequest.schemaName())).executeUpdate();

            //Set Admin email and password
            Account schemaAdmin = setAdminCredentials(appRequest);

            //insert domains and permissions
            insertDomainsAndPermissions(appRequest, schemaAdmin);

            //Switch to authorization schema
            em.createNativeQuery("SET SCHEMA '%s'".formatted(Constants.PUBLIC_SCHEMA)).executeUpdate();

            //Insert application record
            Application application = Application.from(appRequest);
            application = applicationRepository.save(application);

            log.info("Provisioned new schema: {}", appRequest.name());
            return application;
        } catch (Exception e) {
            log.error("Error trying to provision schema. Trace:", e);
            throw new ConflictException(String.format("Error provisioning schema. Message: %s", e.getMessage()));
        } finally {
            em.createNativeQuery("SET SCHEMA '%s'".formatted(Constants.PUBLIC_SCHEMA)).executeUpdate();
        }
    }

    private Account setAdminCredentials(ApplicationDTO.Create appRequest) throws ConflictException, IOException {
        log.info("Setting Admin credentials");
        String adminEmail = appRequest.schemaName() + "@seven.com";
        String adminPassword = UUID.randomUUID().toString();

        Account admin = accountRepository.findByEmail(adminEmail).orElseThrow(() -> new ConflictException("Elevated user not found please contact administrator"));
        admin.setPassword(bCryptPasswordEncoder.encode(adminPassword));

        admin = accountRepository.save(admin);

        //Create credentials file
        Path userHomeDir = Paths.get(System.getProperty("user.home"));
        Path credentialsFilePath = userHomeDir.resolve(appRequest.schemaName() + "_credentials.txt");
        Files.deleteIfExists(credentialsFilePath);
        File credentialsFile = Files.createFile(credentialsFilePath).toFile();

        try (FileOutputStream fos = new FileOutputStream(credentialsFile)) {
            fos.write("Username:%s\nPassword:%s".formatted(adminEmail, adminPassword).getBytes(StandardCharsets.UTF_8));
        }
        log.info("Credentials file: {}", credentialsFilePath.toAbsolutePath());
        return admin;
    }

    private void insertDomainsAndPermissions(ApplicationDTO.Create appRequest, Account schemaAdmin) {
        log.info("Inserting Domains and related permissions");
        List<Domain> domains = appRequest.domains().stream().map(domainRequest -> {
            Domain d = Domain.from(domainRequest);
            d.getPermissions().forEach(permission -> permission.setDomain(d));
            return d;
        }).toList();
        domainRepository.saveAll(domains);
        log.info("Domains and related permissions inserted successfully");
    }

    /**
     * This runs the script responsible for dropping an application's schema
     *
     * @param app
     * @throws IOException
     */
    public void dropSchema(Application app) {
        try {
            log.info("Dropping schema: {} in DB", app.getName());

            Flyway tenantFlyway = buildTenantFlyway(dataSource, app);
            tenantFlyway.clean();

            log.info("Schema: {} (if exists) dropped successfully", app.getName());
        } catch (Exception e) {
            log.error("Error dropping schema {}. Trace:", app.getName(), e);
            throw e;
        }
    }

    public Flyway buildTenantFlyway(DataSource dataSource, Application app) {
        return buildTenantFlyway(dataSource, app.getSchemaName());
    }

    public Flyway buildTenantFlyway(DataSource dataSource, ApplicationDTO.Create appRequest) {
        return buildTenantFlyway(dataSource, appRequest.schemaName());
    }

    public Flyway buildTenantFlyway(DataSource dataSource, String schemaName) {
        log.info("Instantiating flyway for Schema: {} in DB", schemaName);
        String dbVendor = TenantContext.getCurrentDbVendor();
        log.info("DB Vendor: {}", dbVendor);

        ClassicConfiguration flywayConfig = new ClassicConfiguration();
        flywayConfig.setDataSource(dataSource);
        flywayConfig.setSchemas(new String[]{schemaName});
        flywayConfig.setValidateOnMigrate(true);
        flywayConfig.setLocations(new Location(String.format(Constants.TENANT_MIGRATION_SCRIPTS_PATH, dbVendor)));
        flywayConfig.setCleanDisabled(false);
        log.info("Instantiated flyway for Schema: {} in DB", schemaName);
        return new Flyway(flywayConfig);
    }
}