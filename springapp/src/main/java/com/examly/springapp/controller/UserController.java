package com.examly.springapp.controller;

import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.LoginResponse;
import com.examly.springapp.dto.RefreshTokenRequest;
import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.dto.UserRequest;
import com.examly.springapp.dto.UserResponse;
import com.examly.springapp.model.User;
import com.examly.springapp.security.JwtUtil;
import com.examly.springapp.service.RefreshTokenService;
import com.examly.springapp.service.RefreshTokenService.RotationResult;
import com.examly.springapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public UserController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        User saved = userService.registerUser(request);
        return ResponseEntity.ok(toUserResponse(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        User user = userService.getUserByEmail(loginRequest.getEmail());
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        String role = (user.getRole() != null && !user.getRole().isBlank())
                ? user.getRole() : "ROLE_USER";

        String accessToken   = jwtUtil.generateToken(user.getEmail(), role);
        String rawRefreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        return ResponseEntity.ok(new LoginResponse(accessToken, rawRefreshToken, role));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return user != null ? ResponseEntity.ok(toUserResponse(user))
                            : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(u -> ResponseEntity.ok(toUserResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.isOwner(#id, #authentication.name)")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserRequest request,
                                                   Authentication authentication) {
        return userService.updateUser(id, request)
                .map(u -> ResponseEntity.ok(toUserResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> promoteUser(@PathVariable Long id,
                                                    @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || (!role.equals("ROLE_PUBLISHER") && !role.equals("ROLE_ADMIN") && !role.equals("ROLE_USER"))) {
            return ResponseEntity.badRequest().build();
        }
        return userService.promoteUser(id, role)
                .map(u -> ResponseEntity.ok(toUserResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        refreshTokenService.revokeAllTokensForUser(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken();
        if (rawToken == null || rawToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing refresh token"));
        }

        try {
            RotationResult result = refreshTokenService.rotateRefreshToken(rawToken);
            User user = result.getUser();

            String role = (user.getRole() != null && !user.getRole().isBlank())
                    ? user.getRole() : "ROLE_USER";

            String newAccessToken   = jwtUtil.generateToken(user.getEmail(), role);
            String newRefreshToken  = result.getNewRawToken();

            return ResponseEntity.ok(new LoginResponse(newAccessToken, newRefreshToken, role));
        } catch (com.examly.springapp.exception.UnauthorizedException ex) {
            return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return user != null ? ResponseEntity.ok(toUserResponse(user))
                            : ResponseEntity.notFound().build();
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
