package com.examly.springapp.dto;

public class AdminDashboardResponse {

    private int totalUsers;
    private int totalPublishers;
    private int totalNormalUsers;
    private int totalAdmins;
    private int totalBooks;
    private int totalReviews;
    private double averageRating;

    public AdminDashboardResponse() {}

    public AdminDashboardResponse(int totalUsers, int totalPublishers, int totalNormalUsers,
                                   int totalAdmins, int totalBooks, int totalReviews, double averageRating) {
        this.totalUsers = totalUsers;
        this.totalPublishers = totalPublishers;
        this.totalNormalUsers = totalNormalUsers;
        this.totalAdmins = totalAdmins;
        this.totalBooks = totalBooks;
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
    }

    public int getTotalUsers()       { return totalUsers; }
    public void setTotalUsers(int v) { this.totalUsers = v; }

    public int getTotalPublishers()       { return totalPublishers; }
    public void setTotalPublishers(int v) { this.totalPublishers = v; }

    public int getTotalNormalUsers()       { return totalNormalUsers; }
    public void setTotalNormalUsers(int v) { this.totalNormalUsers = v; }

    public int getTotalAdmins()       { return totalAdmins; }
    public void setTotalAdmins(int v) { this.totalAdmins = v; }

    public int getTotalBooks()       { return totalBooks; }
    public void setTotalBooks(int v) { this.totalBooks = v; }

    public int getTotalReviews()       { return totalReviews; }
    public void setTotalReviews(int v) { this.totalReviews = v; }

    public double getAverageRating()       { return averageRating; }
    public void setAverageRating(double v) { this.averageRating = v; }
}
