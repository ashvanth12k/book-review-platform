package com.examly.springapp.repository;

import com.examly.springapp.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBookId(Long bookId);

    List<Review> findByUserId(Long userId);

    Optional<Review> findByBookIdAndUserId(Long bookId, Long userId);

    @Query("SELECT DISTINCT r.bookId FROM Review r WHERE r.userId = :userId")
    Set<Long> findDistinctBookIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.bookId = :bookId")
    Double avgRatingByBookId(@Param("bookId") Long bookId);

    @Query("SELECT AVG(r.rating) FROM Review r")
    Double overallAvgRating();

    @Query("SELECT COUNT(r) FROM Review r")
    long countAll();
}