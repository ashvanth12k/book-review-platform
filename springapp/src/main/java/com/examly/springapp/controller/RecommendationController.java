package com.examly.springapp.controller;

import com.examly.springapp.model.Book;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    @Autowired
    public RecommendationController(RecommendationService recommendationService,
                                    UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Book>> getRecommendations(
            @RequestParam(required = false) String topic,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        List<Book> result = (topic != null && !topic.isBlank())
                ? recommendationService.getRecommendationsByTopic(topic, userId)
                : recommendationService.getRecommendations(userId);
        return ResponseEntity.ok(result);
    }

    private Long getUserId(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException(
                        "Authenticated user not found: " + authentication.getName()));
    }
}
