package com.examly.springapp.service;

import com.examly.springapp.model.Book;
import com.examly.springapp.model.Review;
import com.examly.springapp.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final BookService bookService;
    private final ReviewRepository reviewRepository;

    @Autowired
    public RecommendationService(BookService bookService, ReviewRepository reviewRepository) {
        this.bookService = bookService;
        this.reviewRepository = reviewRepository;
    }

    public List<Book> getRecommendations(Long userId) {
        List<Book> allBooks = bookService.getAllBooksWithAvgRating();

        Set<Long> userReviewedIds = reviewRepository.findDistinctBookIdsByUserId(userId);

        List<Review> userReviews = reviewRepository.findByUserId(userId);

        if (userReviews.isEmpty()) {
            return allBooks.stream()
                    .filter(b -> b.getAverageRating() != null)
                    .sorted(Comparator.comparingDouble(Book::getAverageRating).reversed())
                    .collect(Collectors.toList());
        }

        Set<Long> highlyRatedBookIds = userReviews.stream()
                .filter(r -> r.getRating() >= 4)
                .map(Review::getBookId)
                .collect(Collectors.toSet());

        Set<String> preferredGenres = allBooks.stream()
                .filter(b -> highlyRatedBookIds.contains(b.getId()))
                .map(Book::getGenre)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Book> candidates = allBooks.stream()
                .filter(b -> !userReviewedIds.contains(b.getId()))
                .collect(Collectors.toList());

        List<Book> personalized = candidates.stream()
                .filter(b -> preferredGenres.contains(b.getGenre()))
                .filter(b -> b.getAverageRating() != null)
                .sorted(Comparator.comparingDouble(Book::getAverageRating).reversed())
                .collect(Collectors.toList());

        if (!personalized.isEmpty()) {
            return personalized;
        }

        return candidates.stream()
                .filter(b -> b.getAverageRating() != null)
                .sorted(Comparator.comparingDouble(Book::getAverageRating).reversed())
                .collect(Collectors.toList());
    }

    public List<Book> getRecommendationsByTopic(String topic, Long userId) {
        Set<Long> userReviewedIds = reviewRepository.findDistinctBookIdsByUserId(userId);

        List<Book> topicBooks = bookService.searchByTopic(topic).stream()
                .map(bookService::enrichWithAvgRating)
                .collect(Collectors.toList());

        List<Book> unreviewed = topicBooks.stream()
                .filter(b -> !userReviewedIds.contains(b.getId()))
                .filter(b -> b.getAverageRating() != null)
                .sorted(Comparator.comparingDouble(Book::getAverageRating).reversed())
                .collect(Collectors.toList());

        return unreviewed.isEmpty() ? topicBooks : unreviewed;
    }
}
