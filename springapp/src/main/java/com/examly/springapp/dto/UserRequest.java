package com.examly.springapp.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * DTO for user update (PUT /api/users/{id}).
 * - Does NOT contain a role field — role changes are admin-only via /promote.
 * - Password is optional on update; omit to keep existing password unchanged.
 */
public class UserRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    /** Optional. If blank, the existing password is kept unchanged. */
    private String password;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
