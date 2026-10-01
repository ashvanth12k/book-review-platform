package com.examly.springapp.dto;

import javax.validation.constraints.NotBlank;

/**
 * Request body for POST /api/users/refresh-token.
 * The client sends the raw refresh token it received at login (or last rotation).
 */
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken must not be blank")
    private String refreshToken;

    public RefreshTokenRequest() {}

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
