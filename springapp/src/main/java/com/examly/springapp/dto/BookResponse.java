package com.examly.springapp.dto;

public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String genre;
    private String description;
    private Double averageRating;
    private Long publisherId;

    public BookResponse() {}

    public BookResponse(Long id, String title, String author, String genre,
                        String description, Double averageRating) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.description = description;
        this.averageRating = averageRating;
    }

    public BookResponse(Long id, String title, String author, String genre,
                        String description, Double averageRating, Long publisherId) {
        this(id, title, author, genre, description, averageRating);
        this.publisherId = publisherId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }
}
