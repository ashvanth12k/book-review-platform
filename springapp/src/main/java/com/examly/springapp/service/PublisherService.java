package com.examly.springapp.service;

import com.examly.springapp.dto.BookRequest;
import com.examly.springapp.model.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PublisherService {

    private final BookService bookService;

    @Autowired
    public PublisherService(BookService bookService) {
        this.bookService = bookService;
    }

    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    public Book createBook(BookRequest request, String publisherEmail) {
        return bookService.saveBook(request, publisherEmail);
    }

    public Optional<Book> updateBook(Long id, BookRequest request, String publisherEmail) {
        return bookService.updateBook(id, request, publisherEmail, false);
    }

    public boolean deleteBook(Long id, String publisherEmail) {
        return bookService.deleteBook(id, publisherEmail, false);
    }
}
