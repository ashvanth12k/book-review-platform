package com.examly.springapp.dto;

import javax.validation.constraints.NotBlank;

public class BookRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    private String genre;
    private String description;

    private String publisherEmail;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPublisherEmail() { return publisherEmail; }
    public void setPublisherEmail(String publisherEmail) { this.publisherEmail = publisherEmail; }
}
