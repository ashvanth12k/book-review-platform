package com.examly.springapp.dto;

public class ReviewResponse {

    private Long id;
    private Long bookId;
    private Long userId;
    private String reviewText;
    private int rating;

    public ReviewResponse() {}

    public ReviewResponse(Long id, Long bookId, Long userId, String reviewText, int rating) {
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
