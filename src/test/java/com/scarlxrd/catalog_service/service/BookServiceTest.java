package com.scarlxrd.catalog_service.service;

import com.scarlxrd.catalog_service.dto.*;
import com.scarlxrd.catalog_service.entity.Book;
import com.scarlxrd.catalog_service.exception.BookAlreadyExistsException;
import com.scarlxrd.catalog_service.exception.BookNotExistsException;
import com.scarlxrd.catalog_service.exception.InsufficientStockException;
import com.scarlxrd.catalog_service.mapper.BookMapper;
import com.scarlxrd.catalog_service.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository repository;

    @Mock
    private BookMapper mapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BookService bookService;

    private Book book;
    private CreateBookDTO createDTO;
    private BookResponseDTO responseDTO;
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(UUID.randomUUID());
        book.setTitle("Kubernetes: Up and Running");
        book.setAuthor("Brendan Burns");
        book.setIsbn("9781492046530");
        book.setPrice(new BigDecimal("180.00"));
        book.setStock(15);

        createDTO = new CreateBookDTO();
        createDTO.setTitle("Kubernetes: Up and Running");
        createDTO.setAuthor("Brendan Burns");
        createDTO.setIsbn("9781492046530");
        createDTO.setPrice(new BigDecimal("180.00"));
        createDTO.setStock(15);

        responseDTO = new BookResponseDTO();
        responseDTO.setId(book.getId());
        responseDTO.setTitle(book.getTitle());
        responseDTO.setAuthor(book.getAuthor());
        responseDTO.setIsbn(book.getIsbn());
        responseDTO.setPrice(book.getPrice());
        responseDTO.setStock(book.getStock());
    }

    @Test
    @DisplayName("Deve criar book e retornar BookResponseDTO")
    void shouldCreateBookAndReturnResponse() {
        when(repository.findByIsbn(createDTO.getIsbn())).thenReturn(Optional.empty());
        when(mapper.toEntity(createDTO)).thenReturn(book);
        when(repository.save(book)).thenReturn(book);
        when(mapper.toResponse(book)).thenReturn(responseDTO);

        BookResponseDTO result = bookService.create(createDTO);

        assertThat(result).isNotNull();
        assertThat(result.getIsbn()).isEqualTo(createDTO.getIsbn());
        assertThat(result.getTitle()).isEqualTo(createDTO.getTitle());
        verify(repository).save(book);
    }

    @Test
    @DisplayName("Deve lançar BookAlreadyExistsException quando ISBN já existir")
    void shouldThrowWhenIsbnAlreadyExists() {
        when(repository.findByIsbn(createDTO.getIsbn())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.create(createDTO))
                .isInstanceOf(BookAlreadyExistsException.class)
                .hasMessage("ISBN already exists");

        verify(repository, never()).save(any());
    }


    @Test
    @DisplayName("Deve retornar página de books")
    void shouldReturnPageOfBooks() {
        Page<Book> bookPage = new PageImpl<>(List.of(book));
        when(repository.findAll(pageable)).thenReturn(bookPage);
        when(mapper.toResponse(book)).thenReturn(responseDTO);

        Page<BookResponseDTO> result = bookService.getAllPage(pageable);

        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getIsbn()).isEqualTo(book.getIsbn());
    }

    @Test
    @DisplayName("Deve retornar página vazia quando não houver books")
    void shouldReturnEmptyPageWhenNoBooksExist() {
        when(repository.findAll(pageable)).thenReturn(Page.empty());

        Page<BookResponseDTO> result = bookService.getAllPage(pageable);

        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("Deve retornar books filtrados por author")
    void shouldReturnBooksByAuthor() {
        Page<Book> bookPage = new PageImpl<>(List.of(book));
        when(repository.findByAuthorContainingIgnoreCase("Brendan", pageable)).thenReturn(bookPage);
        when(mapper.toResponse(book)).thenReturn(responseDTO);

        Page<BookResponseDTO> result = bookService.searchByAuthor("Brendan", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getAuthor()).isEqualTo(book.getAuthor());
    }

    @Test
    @DisplayName("Deve retornar página vazia quando author não encontrado")
    void shouldReturnEmptyPageWhenAuthorNotFound() {
        when(repository.findByAuthorContainingIgnoreCase("inexistente", pageable))
                .thenReturn(Page.empty());

        Page<BookResponseDTO> result = bookService.searchByAuthor("inexistente", pageable);

        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("Deve retornar books filtrados por title")
    void shouldReturnBooksByTitle() {
        Page<Book> bookPage = new PageImpl<>(List.of(book));
        when(repository.findByTitleContainingIgnoreCase("Kubernetes", pageable)).thenReturn(bookPage);
        when(mapper.toResponse(book)).thenReturn(responseDTO);

        Page<BookResponseDTO> result = bookService.searchByTitle("Kubernetes", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo(book.getTitle());
    }

    @Test
    @DisplayName("Deve retornar página vazia quando title não encontrado")
    void shouldReturnEmptyPageWhenTitleNotFound() {
        when(repository.findByTitleContainingIgnoreCase("inexistente", pageable))
                .thenReturn(Page.empty());

        Page<BookResponseDTO> result = bookService.searchByTitle("inexistente", pageable);

        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("Deve retornar books filtrados por faixa de preço")
    void shouldReturnBooksByPriceRange() {
        Page<Book> bookPage = new PageImpl<>(List.of(book));
        when(repository.findByPriceBetween(100.0, 200.0, pageable)).thenReturn(bookPage);
        when(mapper.toResponse(book)).thenReturn(responseDTO);

        Page<BookResponseDTO> result = bookService.filterByPrice(100.0, 200.0, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getPrice()).isEqualByComparingTo(new BigDecimal("180.00"));
    }

    @Test
    @DisplayName("Deve retornar página vazia quando não houver books na faixa de preço")
    void shouldReturnEmptyPageWhenNoBooksInPriceRange() {
        when(repository.findByPriceBetween(500.0, 1000.0, pageable)).thenReturn(Page.empty());

        Page<BookResponseDTO> result = bookService.filterByPrice(500.0, 1000.0, pageable);

        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("Deve diminuir o stock corretamente")
    void shouldDecreaseStockCorrectly() {
        when(repository.findById(book.getId())).thenReturn(Optional.of(book));

        bookService.decreaseStock(book.getId().toString(), 5);

        assertThat(book.getStock()).isEqualTo(10); // 15 - 5
        verify(repository).save(book);
    }

    @Test
    @DisplayName("Deve lançar BookNotExistsException quando book não encontrado no decreaseStock")
    void shouldThrowWhenBookNotFoundOnDecreaseStock() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.decreaseStock(id.toString(), 5))
                .isInstanceOf(BookNotExistsException.class)
                .hasMessage("Book not found");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar InsufficientStockException  quando stock insuficiente")
    void shouldThrowWhenInsufficientStock() {
        when(repository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.decreaseStock(book.getId().toString(), 99))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock");

        verify(repository, never()).save(any());
    }


    @Test
    @DisplayName("Deve publicar BookValidatedEvent com available=true quando stock suficiente")
    void shouldPublishAvailableTrueWhenStockSufficient() {
        BookValidationRequest request = new BookValidationRequest();
        request.setOrderId(UUID.randomUUID());
        request.setBookId(book.getId());
        request.setQuantity(5);

        when(repository.findById(book.getId())).thenReturn(Optional.of(book));

        bookService.processValidation(request);

        ArgumentCaptor<BookValidatedEvent> captor = ArgumentCaptor.forClass(BookValidatedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("book.events"), eq("book.validated"), captor.capture());

        BookValidatedEvent event = captor.getValue();
        assertThat(event.getOrderId()).isEqualTo(request.getOrderId());
        assertThat(event.getBookId()).isEqualTo(book.getId());
        assertThat(event.isAvailable()).isTrue();
        assertThat(event.getQuantity()).isEqualTo(5);
        assertThat(event.getPrice()).isEqualByComparingTo(book.getPrice());
    }

    @Test
    @DisplayName("Deve publicar BookValidatedEvent com available=false quando stock insuficiente")
    void shouldPublishAvailableFalseWhenStockInsufficient() {
        BookValidationRequest request = new BookValidationRequest();
        request.setOrderId(UUID.randomUUID());
        request.setBookId(book.getId());
        request.setQuantity(99); // maior que o stock de 15

        when(repository.findById(book.getId())).thenReturn(Optional.of(book));

        bookService.processValidation(request);

        ArgumentCaptor<BookValidatedEvent> captor = ArgumentCaptor.forClass(BookValidatedEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("book.events"), eq("book.validated"), captor.capture());

        assertThat(captor.getValue().isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Deve lançar BookNotExistsException quando book não encontrado na validação")
    void shouldThrowWhenBookNotFoundOnValidation() {
        BookValidationRequest request = new BookValidationRequest();
        request.setOrderId(UUID.randomUUID());
        request.setBookId(UUID.randomUUID());
        request.setQuantity(1);

        when(repository.findById(request.getBookId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.processValidation(request))
                .isInstanceOf(BookNotExistsException.class)
                .hasMessage("Book not found");

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), Optional.ofNullable(any()));
    }
}