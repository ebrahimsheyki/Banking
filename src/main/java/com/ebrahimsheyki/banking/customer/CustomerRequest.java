package com.ebrahimsheyki.banking.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
    @NotBlank(message = "firstName must not be blank") @Size(max = 100,
        message = "firstName must not exceed 100 characters") String firstName,

    @NotBlank(message = "lastName must not be blank") @Size(max = 100,
        message = "lastName must not exceed 100 characters") String lastName,

    @NotBlank(message = "email must not be blank") @Email(message = "email must be valid") @Size(
        max = 255, message = "email must not exceed 255 characters") String email) {

}