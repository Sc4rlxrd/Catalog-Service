package com.scarlxrd.catalog_service.controller;

import com.scarlxrd.catalog_service.exception.BookAlreadyExistsException;
import com.scarlxrd.catalog_service.exception.RateLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.ZonedDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 400 - Erros de validação do @Valid (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("Um ou mais campos são inválidos");

        ex.getBindingResult().getFieldErrors().forEach(err ->
                problem.setProperty(err.getField(), err.getDefaultMessage())
        );

        return problem;
    }

    // 500 - Erros não tratados
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal server error");
        problem.setDetail("Ocorreu um erro inesperado");
        return problem;
    }

    // 409 - Livro já existe
    @ExceptionHandler(BookAlreadyExistsException.class)
    public ProblemDetail handleClientAlreadyExists(BookAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Book already exists");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    // 429 - enviou muitas solicitações num curto período.
    @ExceptionHandler(RateLimitException.class)
    public ProblemDetail handleTooManyRequests(RateLimitException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problem.setTitle("Too Many Requests");
        problem.setDetail(ex.getMessage());
        problem.setProperty("timestamp", ZonedDateTime.now());
        return problem;
    }
}
