package com.scarlxrd.catalog_service.controller;

import com.scarlxrd.catalog_service.dto.BookResponseDTO;
import com.scarlxrd.catalog_service.dto.CreateBookDTO;
import com.scarlxrd.catalog_service.service.BookService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/books")
@Tag(name = "Books", description = "Gerenciamento do catálogo de livros")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastrar livro", description = "Cria um novo livro no catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Livro criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "ISBN já cadastrado")
    })
    @PostMapping
    public ResponseEntity<BookResponseDTO> create(@RequestBody @Valid CreateBookDTO dto){

        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @Operation(summary = "Listar livros", description = "Retorna todos os livros paginados")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public Page<BookResponseDTO> getAllPage(
            @ParameterObject
            @PageableDefault(size = 10, page = 0)
            Pageable pageable){
        return service.getAllPage(pageable);
    }

    @Operation(summary = "Buscar por título", description = "Busca livros pelo título (case insensitive)")
    @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso")
    @GetMapping("/search/title")
    public Page<BookResponseDTO> searchByTitle(
            @ParameterObject
            @Parameter(description = "Título ou parte do título", example = "Kubernetes")
            @RequestParam String title,
            Pageable pageable){
        return service.searchByTitle(title,pageable);
    }

    @Operation(summary = "Buscar por autor", description = "Busca livros pelo autor (case insensitive)")
    @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso")
    @GetMapping("/search/author")
    public Page<BookResponseDTO> searchByAuthor(
            @ParameterObject
            @Parameter(description = "Nome ou parte do nome do autor", example = "Brendan Burns")
            @RequestParam String author,
            Pageable pageable){
        return service.searchByAuthor(author,pageable);
    }

    @Operation(summary = "Filtrar por preço", description = "Filtra livros por faixa de preço")
    @ApiResponse(responseCode = "200", description = "Filtro aplicado com sucesso")
    @GetMapping("/filter")
    public Page<BookResponseDTO> filterByPrice(
            @ParameterObject
            @Parameter(description = "Preço mínimo", example = "100.00") @RequestParam Double min ,
            @Parameter(description = "Preço máximo", example = "300.00") @RequestParam Double max,
            Pageable pageable) {

        return service.filterByPrice(min, max, pageable);
    }

    @Operation(summary = "Diminuir estoque", description = "Reduz a quantidade em estoque de um livro")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoque atualizado"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado"),
            @ApiResponse(responseCode = "400", description = "Estoque insuficiente")
    })
    @PatchMapping("/{id}/stock/decrease")
    public void decreaseStock(
            @Parameter(description = "ID do livro", example = "f89426b9-6dd0-4351-9a57-144d3e3dc090")
            @PathVariable String id,
            @Parameter(description = "Quantidade a diminuir", example = "5")
            @RequestParam int quantity) {

        service.decreaseStock(id, quantity);
    }

    @Operation(summary = "Meus favoritos", description = "Retorna os livros favoritos do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favoritos retornados"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/my-favorites")
    public ResponseEntity<?> getFavorites(
            @Parameter(description = "Email do usuário injetado pelo gateway", hidden = true)
            @RequestHeader("X-User-Email") String userEmail
    ) {

        return ResponseEntity.ok("Buscando favoritos no banco de dados para: " + userEmail);
    }

}
