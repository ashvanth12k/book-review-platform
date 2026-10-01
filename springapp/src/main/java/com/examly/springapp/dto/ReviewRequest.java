package com.examly.springapp.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class ReviewRequest {

    private Long bookId;

    private String reviewText;

    @Min(1) @Max(5)
    private int rating;

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
}
