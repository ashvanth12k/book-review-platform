package com.examly.springapp.controller;

import com.examly.springapp.dto.ReviewRequest;
import com.examly.springapp.dto.ReviewResponse;
import com.examly.springapp.model.Review;
import com.examly.springapp.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(@Valid @RequestBody ReviewRequest request,
                                                    Authentication authentication) {
        Review saved = reviewService.upsertReview(request, authentication.getName());
        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping
    public List<ReviewResponse> getReviews(@RequestParam(required = false) Long bookId) {
        List<Review> reviews = (bookId != null)
                ? reviewService.getReviewsByBookId(bookId)
                : reviewService.getAllReviews();
        return reviews.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(r -> ResponseEntity.ok(toResponse(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id,
                                                       @Valid @RequestBody ReviewRequest request,
                                                       Authentication authentication) {
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        return reviewService.updateReview(id, request, authentication.getName(), isAdmin)
                .map(r -> ResponseEntity.ok(toResponse(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id,
                                             Authentication authentication) {
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        return reviewService.deleteReview(id, authentication.getName(), isAdmin)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getBookId(), r.getUserId(), r.getReviewText(), r.getRating());
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
