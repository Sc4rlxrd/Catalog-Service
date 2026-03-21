package com.scarlxrd.catalog_service.controller;

import com.scarlxrd.catalog_service.dto.BookResponseDTO;
import com.scarlxrd.catalog_service.dto.CreateBookDTO;
import com.scarlxrd.catalog_service.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BookResponseDTO> create(@RequestBody @Valid CreateBookDTO dto){

        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping
    public Page<BookResponseDTO> getAllPage(@PageableDefault(size = 10, page = 0)
            Pageable pageable){
        return service.getAllPage(pageable);
    }

    @GetMapping("/search/title")
    public Page<BookResponseDTO> searchByTitle(@RequestParam String title, Pageable pageable){
        return service.searchByTitle(title,pageable);
    }

    @GetMapping("/search/author")
    public Page<BookResponseDTO> searchByAuthor(@RequestParam String author, Pageable pageable){
        return service.searchByAuthor(author,pageable);
    }

    @GetMapping("/filter")
    public Page<BookResponseDTO> filterByPrice(
            @RequestParam Double min,
            @RequestParam Double max,
            Pageable pageable) {

        return service.filterByPrice(min, max, pageable);
    }

    @PatchMapping("/{id}/stock/decrease")
    public void decreaseStock(
            @PathVariable String id,
            @RequestParam int quantity) {

        service.decreaseStock(id, quantity);
    }

}
