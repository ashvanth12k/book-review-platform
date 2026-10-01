package com.examly.springapp.repository;

import com.examly.springapp.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByGenreIgnoreCase(String genre);
    List<Book> findByTitleContainingIgnoreCase(String title);
    List<Book> findByGenreContainingIgnoreCase(String topic);
    List<Book> findByDescriptionContainingIgnoreCaseOrTitleContainingIgnoreCase(String desc, String title);

    @Query("SELECT b FROM Book b WHERE b.publisher.id = :publisherId")
    List<Book> findByPublisherId(@org.springframework.data.repository.query.Param("publisherId") Long publisherId);

    @Query("SELECT COUNT(b) FROM Book b")
    long countAll();

    @Query("SELECT r.bookId, AVG(r.rating) FROM Review r GROUP BY r.bookId")
    List<Object[]> findAvgRatingsPerBook();
}
