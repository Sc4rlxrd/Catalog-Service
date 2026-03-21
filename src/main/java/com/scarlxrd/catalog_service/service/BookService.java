package com.scarlxrd.catalog_service.service;

import com.scarlxrd.catalog_service.dto.BookResponseDTO;
import com.scarlxrd.catalog_service.dto.CreateBookDTO;
import com.scarlxrd.catalog_service.entity.Book;
import com.scarlxrd.catalog_service.mapper.BookMapper;
import com.scarlxrd.catalog_service.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private  final BookRepository repository;
    private final BookMapper mapper;

    public BookService(BookRepository repository, BookMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public BookResponseDTO create(CreateBookDTO dto){

        repository.findByIsbn(dto.getIsbn()).ifPresent(b ->{
            throw new IllegalArgumentException("ISBN already exists");
        });

        Book book = mapper.toEntity(dto);
        Book saved = repository.save(book);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<BookResponseDTO> getAllPage(Pageable pageable){
        Page<Book> bookPage = repository.findAll(pageable);
        return bookPage.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookResponseDTO> searchByAuthor(String author, Pageable pageable){
        return repository.findByAuthorContainingIgnoreCase(author,pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookResponseDTO> searchByTitle(String title, Pageable pageable){
        return repository.findByTitleContainingIgnoreCase(title, pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookResponseDTO> filterByPrice(Double min, Double max, Pageable pageable){
        return repository.findByPriceBetween(min, max, pageable)
                .map(mapper::toResponse);
    }

    @Transactional
    public void decreaseStock(String bookId, int quantity){
        Book book = repository.findById(java.util.UUID.fromString(bookId))
                .orElseThrow(() -> new RuntimeException("Book not found"));

        if(book.getStock() < quantity){
            throw new RuntimeException("Insufficient stock");
        }

        book.setStock(book.getStock() - quantity);

        repository.save(book);
    }
}
