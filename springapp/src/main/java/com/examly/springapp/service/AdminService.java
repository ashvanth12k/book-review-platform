package com.examly.springapp.service;

import com.examly.springapp.dto.AdminDashboardResponse;
import com.examly.springapp.model.Book;
import com.examly.springapp.model.Review;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.BookRepository;
import com.examly.springapp.repository.RefreshTokenRepository;
import com.examly.springapp.repository.ReviewRepository;
import com.examly.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository        userRepository;
    private final BookRepository        bookRepository;
    private final ReviewRepository      reviewRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder       passwordEncoder;

    @Autowired
    public AdminService(UserRepository userRepository,
                        BookRepository bookRepository,
                        ReviewRepository reviewRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository         = userRepository;
        this.bookRepository         = bookRepository;
        this.reviewRepository       = reviewRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder        = passwordEncoder;
    }

    public AdminDashboardResponse getDashboard() {
        return getStatistics();
    }

    public AdminDashboardResponse getStatistics() {
        long totalUsers      = userRepository.countAll();
        long totalPublishers = userRepository.findByRole("ROLE_PUBLISHER").size();
        long totalNormal     = userRepository.findByRole("ROLE_USER").size();
        long totalAdmins     = userRepository.findByRole("ROLE_ADMIN").size();
        long totalBooks      = bookRepository.countAll();
        long totalReviews    = reviewRepository.countAll();
        Double avgRating     = reviewRepository.overallAvgRating();
        double avg = avgRating != null ? Math.round(avgRating * 100.0) / 100.0 : 0.0;
        return new AdminDashboardResponse(
                (int) totalUsers,
                (int) totalPublishers,
                (int) totalNormal,
                (int) totalAdmins,
                (int) totalBooks,
                (int) totalReviews,
                avg);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getPublishers() {
        return userRepository.findByRole("ROLE_PUBLISHER");
    }

    public User createUser(String name, String email, String password, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already in use: " + email);
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) return false;

        refreshTokenRepository.revokeAllByUserId(id);
        refreshTokenRepository.deleteByUserId(id);

        List<Review> reviews = reviewRepository.findByUserId(id);
        if (!reviews.isEmpty()) {
            reviewRepository.deleteAll(reviews);
        }

        List<Book> ownedBooks = bookRepository.findByPublisherId(id);
        if (!ownedBooks.isEmpty()) {
            ownedBooks.forEach(b -> b.setPublisher(null));
            bookRepository.saveAll(ownedBooks);
        }

        userRepository.deleteById(id);
        return true;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }
}
