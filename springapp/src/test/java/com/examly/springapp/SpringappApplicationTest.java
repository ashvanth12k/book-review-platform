package com.examly.springapp;

import com.examly.springapp.model.Book;
import com.examly.springapp.model.Review;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.BookRepository;
import com.examly.springapp.repository.RefreshTokenRepository;
import com.examly.springapp.repository.ReviewRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.BookService;
import com.examly.springapp.service.ReviewService;
import com.examly.springapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SpringappApplicationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private BookRepository bookRepo;
    @Autowired private ReviewRepository reviewRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private RefreshTokenRepository refreshTokenRepo;
    @Autowired private BookService bookService;
    @Autowired private ReviewService reviewService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private Book testBook;
    private String adminToken;
    private String userToken;
    private User adminUser;
    private User normalUser;

    @BeforeEach
    public void setup() {
        // Must delete in FK dependency order:
        // refresh_token → users (FK: refresh_token.user_id → users.id)
        // review → book (FK: review.book_id → book.id)
        // review → users (FK: review.user_id → users.id)
        refreshTokenRepo.deleteAll();
        reviewRepo.deleteAll();
        bookRepo.deleteAll();
        userRepo.deleteAll();

        // Admin user
        adminUser = new User();
        adminUser.setName("Admin");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRole("ROLE_ADMIN");
        adminUser = userRepo.save(adminUser);
        adminToken = jwtUtil.generateToken(adminUser.getEmail(), "ROLE_ADMIN");

        // Normal user
        normalUser = new User();
        normalUser.setName("User");
        normalUser.setEmail("user@test.com");
        normalUser.setPassword(passwordEncoder.encode("user123"));
        normalUser.setRole("ROLE_USER");
        normalUser = userRepo.save(normalUser);
        userToken = jwtUtil.generateToken(normalUser.getEmail(), "ROLE_USER");

        // Test book
        testBook = new Book();
        testBook.setTitle("Test Book");
        testBook.setAuthor("Author A");
        testBook.setGenre("Fiction");
        testBook.setDescription("Description A");
        testBook = bookRepo.save(testBook);
    }

    // 1 ✅ Context loads
    @Test
    public void SpringBoot_ProjectAnalysisAndUMLDiagram_loadContextAndBeansProperly() {
        assertThat(bookRepo).isNotNull();
        assertThat(reviewRepo).isNotNull();
        assertThat(bookService).isNotNull();
        assertThat(reviewService).isNotNull();
    }

    // 2 ✅ Repository save & find
    @Test
    public void SpringBoot_DatabaseAndSchemaSetup_repositoryLayerBookSaveAndFind() {
        Book book = new Book();
        book.setTitle("Repo Book");
        book.setAuthor("Author B");
        book.setGenre("Drama");
        book.setDescription("Desc B");
        bookRepo.save(book);

        List<Book> books = bookRepo.findAll();
        assertThat(books.stream().anyMatch(b -> "Repo Book".equals(b.getTitle()))).isTrue();
    }

    // 3 ✅ Service save & find
    @Test
    public void SpringBoot_ProjectAnalysisAndUMLDiagram_serviceLayerSaveBook() {
        Book book = new Book();
        book.setTitle("Service Book");
        book.setAuthor("Author C");
        book.setGenre("Horror");
        book.setDescription("Desc C");

        bookService.saveBook(book);
        assertThat(bookRepo.findAll().stream().anyMatch(b -> "Service Book".equals(b.getTitle()))).isTrue();
    }

    // 4 ✅ Get all books endpoint (public)
    @Test
    public void SpringBoot_DevelopCoreAPIsAndBusinessLogic_getAllBooksEndpoint() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // 5 ✅ Post new book endpoint (requires ADMIN or PUBLISHER JWT)
    @Test
    public void SpringBoot_DevelopCoreAPIsAndBusinessLogic_postNewBookEndpoint() throws Exception {
        Map<String, String> body = Map.of(
                "title", "Posted Book",
                "author", "Author D",
                "genre", "Sci-fi",
                "description", "Desc D");

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Posted Book"));

        assertThat(bookRepo.findAll().stream().anyMatch(b -> "Posted Book".equals(b.getTitle()))).isTrue();
    }

    // 5b ✅ Security: unauthenticated book create → 403
    @Test
    public void SpringBoot_Security_unauthenticatedBookCreateReturns403() throws Exception {
        Map<String, String> body = Map.of("title", "Bad Book", "author", "X", "genre", "Y", "description", "Z");
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // 5c ✅ Security: ROLE_USER book create → 403
    @Test
    public void SpringBoot_Security_roleUserBookCreateReturns403() throws Exception {
        Map<String, String> body = Map.of("title", "Bad Book", "author", "X", "genre", "Y", "description", "Z");
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // 6 ✅ Add review repository & service
    @Test
    public void SpringBoot_ProjectAnalysisAndUMLDiagram_repositoryAndServiceSaveReview() {
        Review review = new Review();
        review.setBookId(testBook.getId());
        review.setUserId(adminUser.getId());
        review.setReviewText("Nice book");
        review.setRating(5);

        reviewService.saveReview(review);
        assertThat(reviewRepo.findByBookId(testBook.getId())).hasSize(1);
    }

    // 7 ✅ Get reviews by bookId endpoint (public)
    @Test
    public void SpringBoot_DevelopCoreAPIsAndBusinessLogic_getReviewsByBookIdEndpoint() throws Exception {
        Review review = new Review();
        review.setBookId(testBook.getId());
        review.setUserId(adminUser.getId());
        review.setReviewText("Great book");
        review.setRating(4);
        reviewRepo.save(review);

        mockMvc.perform(get("/api/reviews?bookId=" + testBook.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewText").value("Great book"));
    }

    // 8 ✅ Post review endpoint (requires JWT)
    @Test
    public void SpringBoot_DevelopCoreAPIsAndBusinessLogic_postReviewEndpoint() throws Exception {
        Map<String, Object> body = Map.of(
                "bookId", testBook.getId(),
                "reviewText", "Awesome",
                "rating", 5);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewText").value("Awesome"));

        assertThat(reviewRepo.findByBookId(testBook.getId())).hasSize(1);
    }

    // 9 ✅ Average rating stored as separate field (description NOT mutated)
    @Test
    public void SpringBoot_ProjectAnalysisAndUMLDiagram_averageRatingIsCalculated() {
        Review r1 = new Review();
        r1.setBookId(testBook.getId());
        r1.setUserId(adminUser.getId());
        r1.setReviewText("Good");
        r1.setRating(4);
        reviewRepo.save(r1);

        Review r2 = new Review();
        r2.setBookId(testBook.getId());
        r2.setUserId(normalUser.getId());
        r2.setReviewText("Excellent");
        r2.setRating(5);
        reviewRepo.save(r2);

        List<Book> books = bookService.getAllBooksWithAvgRating();
        Book enriched = books.stream()
                .filter(b -> b.getId().equals(testBook.getId())).findFirst().orElseThrow();

        // averageRating field must be 4.5
        assertThat(enriched.getAverageRating()).isEqualTo(4.5);
        // description must NOT be mutated
        assertThat(enriched.getDescription()).isEqualTo("Description A");
        assertThat(enriched.getDescription()).doesNotContain("Avg Rating");
    }

    // 10 ✅ Empty books list
    @Test
    public void SpringBoot_DevelopCoreAPIsAndBusinessLogic_getAllBooksReturnsEmptyWhenNoneExist() throws Exception {
        bookRepo.deleteAll();
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // 11 ✅ Unauthenticated review create → 403
    @Test
    public void SpringBoot_Security_unauthenticatedReviewCreateReturns403() throws Exception {
        Map<String, Object> body = Map.of("bookId", testBook.getId(), "reviewText", "hi", "rating", 4);
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // 12 ✅ Registration does not allow ROLE_ADMIN
    @Test
    public void SpringBoot_Security_registrationIgnoresClientRole() throws Exception {
        Map<String, String> body = Map.of(
                "name", "Hacker",
                "email", "hacker@test.com",
                "password", "secret123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        User registered = userRepo.findByEmail("hacker@test.com").orElseThrow();
        assertThat(registered.getRole()).isEqualTo("ROLE_USER");
    }
}
