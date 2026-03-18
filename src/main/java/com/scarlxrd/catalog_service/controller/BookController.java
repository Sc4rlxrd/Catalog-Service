package com.scarlxrd.catalog_service.controller;

import com.scarlxrd.catalog_service.dto.BookResponseDTO;
import com.scarlxrd.catalog_service.dto.CreateBookDTO;
import com.scarlxrd.catalog_service.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @PostMapping
    public BookResponseDTO create(@RequestBody @Valid CreateBookDTO dto) {
        return service.create(dto);
    }
    @GetMapping
    public Page<BookResponseDTO> getAllPage(Pageable pageable){
        return service.getAllPage(pageable);
    }

}
