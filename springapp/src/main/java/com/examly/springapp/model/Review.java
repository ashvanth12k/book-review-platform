package com.examly.springapp.model;

import javax.persistence.*;

@Entity
@Table(name = "review",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_review_user_book",
                columnNames = {"book_id", "user_id"}))
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    private String reviewText;

    @Column(nullable = false)
    private int rating;

    public Review() {}

    public Review(Long id, Long bookId, Long userId, String reviewText, int rating) {
        this.id = id;
        this.bookId = bookId;
        this.userId = userId;
        this.reviewText = reviewText;
        this.rating = rating;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
}