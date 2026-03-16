package com.seven.auth.account;

import com.seven.auth.application.Application;
import com.seven.auth.application.ApplicationRepository;
import com.seven.auth.config.threadlocal.TenantContext;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.exception.ClientException;
import com.seven.auth.exception.ConflictException;
import com.seven.auth.exception.NotFoundException;
import com.seven.auth.util.Constants;
import com.seven.auth.util.Pagination;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountService implements UserDetailsService, UserDetailsPasswordService {
    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EntityManager em;
    private final ApplicationRepository applicationRepository;


    public AccountService(AccountRepository accountRepository,
                          BCryptPasswordEncoder passwordEncoder, EntityManager em, ApplicationRepository applicationRepository) {
        this.accountRepository = accountRepository;
        this.bCryptPasswordEncoder = passwordEncoder;
        this.em = em;
        this.applicationRepository = applicationRepository;
    }

    public Page<AccountDTO.Record> getAll(Pagination pagination, AccountDTO.Filter accountFilter) throws AuthorizationException {
        try {
            log.info("Fetching accounts: limit {}, offset {}", pagination.getLimit(), pagination.getOffset());
            Pageable pageable = PageRequest.of(pagination.getLimit(), pagination.getOffset(),
                    Sort.by(
                            Sort.Direction.fromString(pagination.getSortOrder() == null ? "DESC" : pagination.getSortOrder()),
                            pagination.getSortField() == null ? "dateCreated" : pagination.getSortField()
                    ));

            return accountRepository.findAll(AccountSearchSpecification.getAllAndFilter(accountFilter), pageable).map(AccountDTO.Record::from);
        } catch (Exception e) {
            log.error("Unable to fetch accounts. Message: ", e);
            throw new ClientException(e.getMessage());
        }
    }

    public AccountDTO.Record get(UUID id) throws AuthorizationException {
        try {
            log.info("Fetching account: {}", id);
            Account accountFromDb = accountRepository.findById(id).orElseThrow(
                    () -> new NotFoundException("This account does not exist or has been deleted"));

            AccountDTO.Record record = AccountDTO.Record.from(accountFromDb);
            log.info("Account {} successfully retrieved", id);
            return record;
        } catch (AuthorizationException e) {
            log.error("Unable to fetch account: {}. Message: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unable to fetch account: {}. Message: ", id, e);
            throw new ClientException(e.getMessage());
        }
    }

    @Transactional
    public AccountDTO.Record create(AccountDTO.Create accountCreateRequest) throws AuthorizationException {
        try {
            log.info("Creating account: {} in schema: {}", accountCreateRequest.email(), TenantContext.getCurrentTenant());
            if (accountRepository.existsByEmail(accountCreateRequest.email()))
                throw new ConflictException("An account with this email already exists");

            Account account = Account.from(accountCreateRequest);

            //Encode password
            account.setPassword(bCryptPasswordEncoder.encode(accountCreateRequest.password()));

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String principalEmail = isUserUnauthenticated(authentication) ? accountCreateRequest.email(): ((AccountDTO.Record) authentication.getPrincipal()).email();
            account.setCreatedBy(principalEmail);
            account.setUpdatedBy(principalEmail);

            account = accountRepository.saveAndFlush(account);
            AccountDTO.Record record = AccountDTO.Record.from(account);

            log.info("Account {} successfully created ins schema: {}", account.getId(), TenantContext.getCurrentTenant());
            return record;
        } catch (AuthorizationException e) {
            log.error("Unable to create account: {}. Message: {}", accountCreateRequest.email(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unable to create account: {}. Message: ", accountCreateRequest.email(), e);
            throw new ConflictException(e.getMessage());
        }
    }

    private static boolean isUserUnauthenticated(Authentication authentication) {
        return authentication instanceof AnonymousAuthenticationToken || authentication instanceof RememberMeAuthenticationToken;
    }

    @Transactional
    public AccountDTO.Record createSuper(AccountDTO.Create accountCreateRequest) throws AuthorizationException {
        try {
            log.info("Creating Superuser: {} in schema: {}", accountCreateRequest.email(), TenantContext.getCurrentTenant());
            //Persist in public schema
            AccountDTO.Record accountRecord = create(accountCreateRequest);

            //Get all registered schemas/tenants
            Set<String> tenants = applicationRepository.findAll().stream().map(Application::getSchemaName).collect(Collectors.toSet());
            tenants.remove(Constants.PUBLIC_SCHEMA);

            String sqlTemplate = "INSERT INTO \"%s\".auth_account(id, first_name, last_name, email, status,  phone_no, auth_provider, dob, password, is_deleted, date_created, created_by, date_updated, updated_by)" +
                    "VALUES(:id, :firstName, :lastName, :email, :status, :phoneNo, :authProvider, :dob, :password, :isDeleted, CURRENT_TIMESTAMP, :createdBy, CURRENT_TIMESTAMP, :updatedBy);";
            String sql;
            for (String tenant : tenants) {
                boolean exists = (Boolean) em.createNativeQuery("SELECT EXISTS(SELECT 1 FROM \"%s\".auth_account WHERE email = '%s');".formatted(tenant, accountCreateRequest.email())).getSingleResult();
                log.info("Already exists in schema '{}' : {}; {}", tenant, exists, exists ? "Skipping..." : "Creating...");
                if (!exists) {
                    sql = sqlTemplate.formatted(tenant);

                    em.createNativeQuery(sql)
                            .setParameter("id", accountRecord.id())
                            .setParameter("firstName", accountRecord.firstName())
                            .setParameter("lastName", accountRecord.lastName())
                            .setParameter("email", accountRecord.email())
                            .setParameter("status", Account.AccountStatus.INACTIVE.toString())
                            .setParameter("dob", accountRecord.dob())
                            .setParameter("authProvider", AuthProvider.in_house.toString())
                            .setParameter("phoneNo", accountRecord.phoneNo())
                            .setParameter("password", "_")
                            .setParameter("isDeleted", false)
                            .setParameter("createdBy", accountRecord.createdBy())
                            .setParameter("updatedBy", accountRecord.updatedBy())
                            .executeUpdate();
                }
            }

            assignRootRole(accountRecord);
            log.info("Tenant schemas populated with new superuser: {}", accountCreateRequest.email());
            return accountRecord;
        } catch (Exception e) {
            log.error("Unable to create accounts. Message: {}", e.getMessage());
            throw new ConflictException(e.getMessage());
        } finally {
            em.createNativeQuery("SET SCHEMA '%s';".formatted(Constants.PUBLIC_SCHEMA)).executeUpdate();
        }
    }

    private void assignRootRole(AccountDTO.Record accountRecord) {
        log.info("Assigning ROOT role to account: {}",accountRecord.email());

        em.createNativeQuery("INSERT INTO public.auth_assignment(account_email, role_id, date_created, date_updated, created_by, updated_by)\n" +
                        "VALUES(:email, (SELECT id FROM public.auth_role WHERE name = 'ROOT'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :principalEmail, :principalEmail);")
                .setParameter("email", accountRecord.email())
                .setParameter("principalEmail", accountRecord.createdBy())
                .executeUpdate();
        log.info("Assigned ROOT role to account: {} successfully",accountRecord.email());
    }

    public void delete(UUID id) throws AuthorizationException {
        try {
            log.info("Deleting account: {}", id);
            accountRepository.deleteById(id);
            log.info("Account: {} deleted successfully", id);
        } catch (Exception e) {
            log.error("Unable to delete account: {}. Message: {}", id, e.getMessage());
            throw new ConflictException(e.getMessage());
        }
    }

    @Transactional
    public AccountDTO.Record update(UUID id, AccountDTO.Update accountUpdateRequest) throws AuthorizationException {
        try {
            log.info("Modifying account: {}", id);
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Account account could not be found"));

            Account.update(account, accountUpdateRequest);
            accountRepository.save(account);
            AccountDTO.Record record = AccountDTO.Record.from(account);

            log.info("Account {} successfully modified", account.getId());
            return record;
        } catch (AuthorizationException e) {
            log.error("AuthorizationException; Unable to modify account: {}. Message: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unable to modify account: {}. Message: ", id, e);
            throw new ConflictException(e.getMessage());
        }
    }


    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return accountRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("Username not found"));
    }

    @Override
    public UserDetails updatePassword(UserDetails userDetails, String newPassword) {
        Account account = (Account) userDetails;
        account.setPassword(bCryptPasswordEncoder.encode(newPassword));
        account = accountRepository.save(account);
        return account;
    }
}