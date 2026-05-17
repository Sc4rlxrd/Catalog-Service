package com.scarlxrd.catalog_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Dados para cadastro de um novo livro")
public class CreateBookDTO {

    @Schema(description = "Título do livro", example = "Kubernetes: Up and Running")
    @NotBlank
    private String title;

    @Schema(description = "Nome do autor", example = "Brendan Burns")
    @NotBlank
    private String author;

    @Schema(description = "ISBN do livro", example = "9781492046530")
    @NotBlank
    private String isbn;

    @Schema(description = "Preço do livro", example = "180.00")
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    @Schema(description = "Quantidade em estoque", example = "15")
    @NotNull
    @Min(0)
    private Integer stock;
}