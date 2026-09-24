package com.seven.auth.account;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public interface IAccountEntity {
    UUID getId();

    String getFirstName();

    String getLastName();

    String getPhoneNo();

    String getEmail();

    LocalDate getDob();

    ZonedDateTime getDateCreated();

    ZonedDateTime getDateUpdated();

    String getCreatedBy();

    String getUpdatedBy();
    Boolean getIsDeleted();

}
