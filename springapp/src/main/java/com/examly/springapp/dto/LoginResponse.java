package com.examly.springapp.dto;

/**
 * Response body returned on login and on token refresh.
 * Contains ONLY public-facing data — no password, no hash, no internal IDs.
 */
public class LoginResponse {

    private String token;
    private String refreshToken;
    private String role;

    public LoginResponse(String token, String refreshToken, String role) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
