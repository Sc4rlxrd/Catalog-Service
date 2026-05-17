package com.scarlxrd.catalog_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Dados de resposta de um livro")
public class BookResponseDTO {

    @Schema(description = "ID único do livro", example = "f89426b9-6dd0-4351-9a57-144d3e3dc090")
    private UUID id;

    @Schema(description = "Título do livro", example = "Kubernetes: Up and Running")
    private String title;

    @Schema(description = "Nome do autor", example = "Brendan Burns")
    private String author;

    @Schema(description = "ISBN do livro", example = "9781492046530")
    private String isbn;

    @Schema(description = "Preço do livro", example = "180.00")
    private BigDecimal price;

    @Schema(description = "Quantidade em estoque", example = "15")
    private Integer stock;

    @Schema(description = "Data de cadastro", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;
}
