package com.itm.space.backendresources.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @Size(min = 2, max = 30, message = "Username should be between 2 and 30 characters long")
    @NotBlank(message = "Username should not be blank")
    private final String username;
    @Email(message = "Email should be valid", regexp = ".+@.+\\..+")
    @NotBlank(message = "Email should not be blank")
    private final String email;
    @NotBlank(message = "Password should not be blank")
    @Size(min = 4, message = "Password should be greater than 4 characters long")
    private final String password;
    @NotBlank
    private final String firstName;
    @NotBlank
    private final String lastName;
}

