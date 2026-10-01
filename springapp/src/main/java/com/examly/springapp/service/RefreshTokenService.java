package com.examly.springapp.service;

import com.examly.springapp.exception.UnauthorizedException;
import com.examly.springapp.model.RefreshToken;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.RefreshTokenRepository;
import com.examly.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String createRefreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        String rawToken = generateRawToken();
        String tokenHash = sha256Hex(rawToken);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(tokenHash);
        rt.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        rt.setRevoked(false);
        refreshTokenRepository.save(rt);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public Optional<User> validateAndGetUser(String rawToken) {
        String tokenHash = sha256Hex(rawToken);
        return refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(RefreshToken::isValid)
                .map(RefreshToken::getUser);
    }

    @Transactional
    public RotationResult rotateRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = sha256Hex(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!existing.isValid()) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        User user = existing.getUser();

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRawToken = generateRawToken();
        String newTokenHash = sha256Hex(newRawToken);

        RefreshToken newRt = new RefreshToken();
        newRt.setUser(user);
        newRt.setTokenHash(newTokenHash);
        newRt.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        newRt.setRevoked(false);
        refreshTokenRepository.save(newRt);

        return new RotationResult(user, newRawToken);
    }

    @Transactional
    public void revokeAllTokensForUser(String email) {
        userRepository.findByEmail(email).ifPresent(user ->
                refreshTokenRepository.revokeAllByUserId(user.getId()));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static final class RotationResult {
        private final User user;
        private final String newRawToken;

        public RotationResult(User user, String newRawToken) {
            this.user = user;
            this.newRawToken = newRawToken;
        }

        public User getUser() { return user; }
        public String getNewRawToken() { return newRawToken; }
    }
}
