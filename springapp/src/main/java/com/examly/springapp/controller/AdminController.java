package com.examly.springapp.controller;

import com.examly.springapp.dto.AdminDashboardResponse;
import com.examly.springapp.dto.UserResponse;
import com.examly.springapp.model.Book;
import com.examly.springapp.model.Review;
import com.examly.springapp.model.User;
import com.examly.springapp.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        AdminDashboardResponse stats = adminService.getDashboard();
        Map<String, Object> response = Map.of(
                "totalUsers",   stats.getTotalUsers(),
                "totalBooks",   stats.getTotalBooks(),
                "totalReviews", stats.getTotalReviews()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = adminService.getAllUsers().stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String name     = body.get("name");
        String email    = body.get("email");
        String password = body.get("password");
        String role     = body.get("role");

        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()
                || role == null || role.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "name, email, password and role are required"));
        }

        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        if (!role.equals("ROLE_USER")
                && !role.equals("ROLE_PUBLISHER")
                && !role.equals("ROLE_ADMIN")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "role must be ROLE_USER, ROLE_PUBLISHER or ROLE_ADMIN"));
        }

        try {
            User saved = adminService.createUser(name, email, password, role);
            UserResponse resp = new UserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return adminService.deleteUser(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/publishers")
    public ResponseEntity<List<UserResponse>> getPublishers() {
        List<UserResponse> publishers = adminService.getPublishers().stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(publishers);
    }

    @GetMapping("/books")
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(adminService.getAllBooks());
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<Review>> getAllReviews() {
        return ResponseEntity.ok(adminService.getAllReviews());
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        AdminDashboardResponse stats = adminService.getStatistics();
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("totalUsers",       stats.getTotalUsers());
        response.put("totalPublishers",  stats.getTotalPublishers());
        response.put("totalNormalUsers", stats.getTotalNormalUsers());
        response.put("totalBooks",       stats.getTotalBooks());
        response.put("totalReviews",     stats.getTotalReviews());
        response.put("averageRating",    stats.getAverageRating());
        return ResponseEntity.ok(response);
    }
}
