package com.seven.auth.account;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

@Entity
@Table(name="auth_account")
@Data
@ToString
public class Account implements Serializable, UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phoneNo;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "auth_provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider = AuthProvider.in_house;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.INACTIVE;

    @Column
    private LocalDate dob;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(nullable = false)
    private ZonedDateTime dateCreated;

    @UpdateTimestamp
    @Column(nullable = false)
    private ZonedDateTime dateUpdated;

    @Column
    private String createdBy;

    @Column
    private String updatedBy;
    public enum AccountStatus{ACTIVE, INACTIVE}

    public Account() {
    }

    public Account(String firstName, String lastName, String phoneNo, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNo = phoneNo;
        this.email = email;
        this.password = password;
    }

    public Account(AuthProvider authProvider, String firstName, String lastName, String phoneNo, String email, String password) {
        this.authProvider = authProvider;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNo = phoneNo;
        this.email = email;
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }
    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static Account from(AccountDTO.Create req){
        Account account = new Account();
        account.setFirstName(req.firstName());
        account.setLastName(req.lastName());
        account.setEmail(req.email());
        account.setDob(req.dob());
        account.setPhoneNo(req.phoneNo());
        account.setPassword(req.password());
        return account;
    }
    public static Account from(AccountDTO.Record rec){
        Account account = new Account();
        account.setId(rec.id());
        account.setFirstName(rec.firstName());
        account.setLastName(rec.lastName());
        account.setEmail(rec.email());
        account.setDob(rec.dob());
        account.setPhoneNo(rec.phoneNo());
        return account;
    }

    public static void update(Account account, AccountDTO.Update req){
        account.setFirstName(req.firstName());
        account.setLastName(req.lastName());
        account.setDob(req.dob());
        account.setPhoneNo(req.phoneNo());
    }
}
