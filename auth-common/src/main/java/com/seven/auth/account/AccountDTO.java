package com.seven.auth.account;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class AccountDTO {

    @Validated
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "AccountCreateRequest")
    public record Create(
            @NotBlank(message = "Required field")
            @Pattern(regexp = "^[A-Za-z\\-]{2,30}$", message = "First name must be a sequence of 2-30 alphabetic characters")
            String firstName,
            @NotBlank(message = "Required field")
            @Pattern(regexp = "^[A-Za-z\\-]{2,30}$", message = "Last name must be a sequence of 2-30 alphabetic characters")
            String lastName,
            @NotBlank(message = "Required field")
            @Pattern(regexp = "^[+-][0-9]{10,20}$", message = "Invalid phone number format")
            String phoneNo,
            @NotBlank(message = "Required field")
            @Email(regexp = "^\\w{2,}@\\w{2,}\\.\\w{2,}$", message = "Invalid email format")
            String email,
            @Pattern(regexp = ".{8,}", message = "Password must be at least 8 characters long")
            String password,
            @Past(message = "Future and current dates not allowed")
            LocalDate dob
    ){}

    @Validated
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "AccountUpdateRequest")
    public record Update(
            @NotBlank(message = "Required field")
            @Pattern(regexp = "^[A-Za-z\\-]{2,30}$", message = "First name must be a sequence of 2-30 alphabetic characters")
            String firstName,
            @NotBlank(message = "Required field")
            @Pattern(regexp = "^[A-Za-z\\-]{2,30}$", message = "Last name must be a sequence of 2-30 alphabetic characters")
            String lastName,
            @Pattern(regexp = "^[+-][0-9]{10,20}$", message = "Invalid phone number format")
            String phoneNo,
            @NotBlank
            @Email(regexp = "\\w{2,}@\\w{2,}\\.\\w{2,}", message = "Invalid email format")
            String email,
            @Pattern(regexp = "^[A-Za-z\\d'.!@#$%^&*_\\-]{8,}", message = "Password must fulfill all requirements")
            String password,
            @Past(message = "Future and current dates not allowed")
            LocalDate dob
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "AccountFilterRequest")
    public record Filter(
            String name,
            String phoneNo,
            String email,
            LocalDate dob,
            ZonedDateTime dateCreatedFrom,
            ZonedDateTime dateCreatedTo
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "AccountResponse")
    public record Record(
            UUID id,
            String firstName,
            String lastName,
            String phoneNo,
            String email,
            LocalDate dob,
            ZonedDateTime dateCreated,
            ZonedDateTime dateUpdated,
            String createdBy,
            String updatedBy
    ) {
        public static Record from(IAccountEntity account) {
            return new Record(
                    account.getId(),
                    account.getFirstName(),
                    account.getLastName(),
                    account.getPhoneNo(),
                    account.getEmail(),
                    account.getDob(),
                    account.getDateCreated(),
                    account.getDateUpdated(),
                    account.getCreatedBy(),
                    account.getUpdatedBy()
            );
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "AccountMinResponse")
    public record MinRecord(
            String firstName,
            String lastName,
            String email,
            Boolean isDeleted
    ) {
        public static MinRecord from(IAccountEntity account) {
            return new MinRecord(
                    account.getFirstName(),
                    account.getLastName(),
                    account.getEmail(),
                    account.getIsDeleted()
            );
        }
    }
}
