package com.examly.springapp.service;

import com.examly.springapp.dto.ReviewRequest;
import com.examly.springapp.exception.BadRequestException;
import com.examly.springapp.exception.ForbiddenException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Review;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.ReviewRepository;
import com.examly.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    public Review upsertReview(ReviewRequest request, String email) {
        if (request.getBookId() == null) {
            throw new BadRequestException("bookId is required when creating a review.");
        }
        validateRating(request.getRating());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        Optional<Review> existing = reviewRepository.findByBookIdAndUserId(
                request.getBookId(), user.getId());

        Review review = existing.orElseGet(Review::new);
        review.setBookId(request.getBookId());
        review.setUserId(user.getId());
        review.setReviewText(request.getReviewText() != null ? request.getReviewText() : "");
        review.setRating(request.getRating());
        return reviewRepository.save(review);
    }

    public Review saveReview(Review review) {
        validateRating(review.getRating());
        return reviewRepository.save(review);
    }

    public List<Review> getReviewsByBookId(Long bookId) {
        return reviewRepository.findByBookId(bookId);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    public Optional<Review> updateReview(Long reviewId, ReviewRequest request, String email, boolean isAdmin) {
        validateRating(request.getRating());

        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (!isAdmin && !java.util.Objects.equals(existing.getUserId(), user.getId())) {
            throw new ForbiddenException("You do not have permission to update this review.");
        }

        existing.setReviewText(request.getReviewText() != null ? request.getReviewText() : "");
        existing.setRating(request.getRating());
        return Optional.of(reviewRepository.save(existing));
    }

    public boolean deleteReview(Long reviewId, String email, boolean isAdmin) {
        Review existing = reviewRepository.findById(reviewId)
                .orElse(null);
        if (existing == null) return false;

        if (!isAdmin) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
            if (!java.util.Objects.equals(existing.getUserId(), user.getId())) {
                throw new ForbiddenException("You do not have permission to delete this review.");
            }
        }

        reviewRepository.deleteById(reviewId);
        return true;
    }

    public Set<Long> getReviewedBookIdsByUser(Long userId) {
        return reviewRepository.findDistinctBookIdsByUserId(userId);
    }

    public Set<Long> getAllReviewedBookIds() {
        return reviewRepository.findDistinctBookIdsByUserId(-1L);
    }

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5.");
        }
    }
}
