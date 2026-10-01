package com.examly.springapp.service;

import com.examly.springapp.dto.BookRequest;
import com.examly.springapp.dto.BookResponse;
import com.examly.springapp.exception.ForbiddenException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Book;
import com.examly.springapp.model.Review;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.BookRepository;
import com.examly.springapp.repository.ReviewRepository;
import com.examly.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Autowired
    public BookService(BookRepository bookRepository,
                       ReviewRepository reviewRepository,
                       UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    public Book saveBook(BookRequest request, String callerEmail, boolean isAdmin) {
        final String ownerEmail =
                (isAdmin
                        && request.getPublisherEmail() != null
                        && !request.getPublisherEmail().isBlank())
                        ? request.getPublisherEmail()
                        : callerEmail;

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerEmail));

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setGenre(request.getGenre());
        book.setDescription(request.getDescription());
        book.setPublisher(owner);
        return bookRepository.save(book);
    }

    public Book saveBook(BookRequest request, String callerEmail) {
        return saveBook(request, callerEmail, false);
    }

    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Book> getAllBooksWithAvgRating() {
        List<Book> books = bookRepository.findAll();
        if (books.isEmpty()) return books;

        Map<Long, Double> avgByBookId = new HashMap<>();
        List<Object[]> rows = bookRepository.findAvgRatingsPerBook();
        for (Object[] row : rows) {
            Long bookId = ((Number) row[0]).longValue();
            Double avg  = row[1] != null ? ((Number) row[1]).doubleValue() : null;
            if (avg != null) avgByBookId.put(bookId, avg);
        }

        books.forEach(book -> {
            Double avg = avgByBookId.get(book.getId());
            book.setAverageRating(avg);
        });

        return books;
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id).map(book -> {
            Double avg = reviewRepository.avgRatingByBookId(book.getId());
            book.setAverageRating(avg);
            return book;
        });
    }

    public Optional<Book> updateBook(Long id, BookRequest request, String requesterEmail, boolean isAdmin) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));

        if (!isAdmin) {
            User requester = userRepository.findByEmail(requesterEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterEmail));

            Long ownerId = existing.getPublisherId();
            if (ownerId == null || !ownerId.equals(requester.getId())) {
                throw new ForbiddenException("You do not have permission to update this book.");
            }
        }

        existing.setTitle(request.getTitle());
        existing.setAuthor(request.getAuthor());
        existing.setGenre(request.getGenre());
        existing.setDescription(request.getDescription());
        return Optional.of(bookRepository.save(existing));
    }

    public boolean deleteBook(Long id, String requesterEmail, boolean isAdmin) {
        Book existing = bookRepository.findById(id).orElse(null);
        if (existing == null) return false;

        if (!isAdmin) {
            User requester = userRepository.findByEmail(requesterEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterEmail));

            Long ownerId = existing.getPublisherId();
            if (ownerId == null || !ownerId.equals(requester.getId())) {
                throw new ForbiddenException("You do not have permission to delete this book.");
            }
        }

        bookRepository.deleteById(id);
        return true;
    }

    public List<Book> searchByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Book> getBooksByGenre(String genre) {
        return bookRepository.findByGenreIgnoreCase(genre);
    }

    public List<Book> searchByTopic(String topic) {
        List<Book> byGenre   = bookRepository.findByGenreContainingIgnoreCase(topic);
        List<Book> byKeyword = bookRepository
                .findByDescriptionContainingIgnoreCaseOrTitleContainingIgnoreCase(topic, topic);
        return java.util.stream.Stream.concat(byGenre.stream(), byKeyword.stream())
                .collect(java.util.stream.Collectors.toMap(
                        Book::getId, b -> b, (a, b) -> a))
                .values().stream()
                .collect(Collectors.toList());
    }

    public Optional<Book> addRating(Long bookId, int rating, Long userId) {
        return bookRepository.findById(bookId).map(book -> {
            Optional<Review> existing = reviewRepository.findByBookIdAndUserId(bookId, userId);
            Review review = existing.orElseGet(Review::new);
            review.setBookId(bookId);
            review.setUserId(userId);
            review.setRating(rating);
            if (review.getReviewText() == null) review.setReviewText("");
            reviewRepository.save(review);
            book.setAverageRating(reviewRepository.avgRatingByBookId(bookId));
            return book;
        });
    }

    public Optional<Book> updateRating(Long bookId, int rating, Long userId) {
        return addRating(bookId, rating, userId);
    }

    public Book enrichWithAvgRating(Book book) {
        Double avg = reviewRepository.avgRatingByBookId(book.getId());
        book.setAverageRating(avg);
        return book;
    }

    public double getOverallAverageRating() {
        Double avg = reviewRepository.overallAvgRating();
        return avg != null ? avg : 0.0;
    }

    public BookResponse toBookResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getDescription(),
                book.getAverageRating(),
                book.getPublisherId()
        );
    }
}
