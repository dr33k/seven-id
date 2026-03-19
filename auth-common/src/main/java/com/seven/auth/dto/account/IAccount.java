package com.seven.auth.dto.account;


import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class IAccount{
    public interface Request{
        String firstName();
        String lastName();
        String phoneNo();
        String email();
        LocalDate dob();
    }

    public interface Record {
        UUID id();
        String firstName();
        String lastName();
        String phoneNo();
        String email();
        LocalDate dob();
        ZonedDateTime dateCreated();
        ZonedDateTime dateUpdated();
        String createdBy();
        String updatedBy();
    }
}
