package com.examly.springapp.controller;

import com.examly.springapp.dto.BookRequest;
import com.examly.springapp.model.Book;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final UserRepository userRepository;

    @Autowired
    public BookController(BookService bookService, UserRepository userRepository) {
        this.bookService = bookService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooksWithAvgRating();
    }

    @PostMapping
    @PreAuthorize("hasRole('PUBLISHER') or hasRole('ADMIN')")
    public ResponseEntity<Book> addBook(@Valid @RequestBody BookRequest request,
                                        Authentication authentication) {
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        return ResponseEntity.ok(bookService.saveBook(request, authentication.getName(), isAdmin));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PUBLISHER') or hasRole('ADMIN')")
    public ResponseEntity<Book> updateBook(@PathVariable Long id,
                                           @Valid @RequestBody BookRequest request,
                                           Authentication authentication) {
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        return bookService.updateBook(id, request, authentication.getName(), isAdmin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id,
                                           Authentication authentication) {
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        return bookService.deleteBook(id, authentication.getName(), isAdmin)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(@RequestParam String title) {
        return ResponseEntity.ok(bookService.searchByTitle(title));
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<Book>> getBooksByGenre(@PathVariable String genre) {
        return ResponseEntity.ok(bookService.getBooksByGenre(genre));
    }

    @PostMapping("/{id}/rating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Book> addRating(@PathVariable Long id,
                                          @RequestBody Map<String, Integer> body,
                                          Authentication authentication) {
        Integer rating = body.get("rating");
        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().build();
        }
        Long userId = getUserId(authentication);
        return bookService.addRating(id, rating, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/rating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Book> updateRating(@PathVariable Long id,
                                             @RequestBody Map<String, Integer> body,
                                             Authentication authentication) {
        Integer rating = body.get("rating");
        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().build();
        }
        Long userId = getUserId(authentication);
        return bookService.updateRating(id, rating, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Long getUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + email));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
