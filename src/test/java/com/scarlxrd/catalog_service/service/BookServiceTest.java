package com.scarlxrd.catalog_service.service;

import com.scarlxrd.catalog_service.config.metrics.CatalogMetrics;
import com.scarlxrd.catalog_service.config.metrics.RabbitEventMetrics;
import com.scarlxrd.catalog_service.dto.*;
import com.scarlxrd.catalog_service.entity.Book;
import com.scarlxrd.catalog_service.entity.ProcessedEvent;
import com.scarlxrd.catalog_service.exception.BookAlreadyExistsException;
import com.scarlxrd.catalog_service.exception.BookNotExistsException;
import com.scarlxrd.catalog_service.exception.InsufficientStockException;
import com.scarlxrd.catalog_service.mapper.BookMapper;
import com.scarlxrd.catalog_service.repository.BookRepository;
import com.scarlxrd.catalog_service.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do BookService")
class BookServiceTest {

    @Mock
    private BookRepository repository;

    @Mock
    private BookMapper mapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private CatalogMetrics metrics;

    @Mock
    private RabbitEventMetrics eventMetrics;

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
        createDTO.setTitle(book.getTitle());
        createDTO.setAuthor(book.getAuthor());
        createDTO.setIsbn(book.getIsbn());
        createDTO.setPrice(book.getPrice());
        createDTO.setStock(book.getStock());

        responseDTO = new BookResponseDTO();
        responseDTO.setId(book.getId());
        responseDTO.setTitle(book.getTitle());
        responseDTO.setAuthor(book.getAuthor());
        responseDTO.setIsbn(book.getIsbn());
        responseDTO.setPrice(book.getPrice());
        responseDTO.setStock(book.getStock());
    }

    @Nested
    @DisplayName("Criação de livro")
    class CreateTests {

        @Test
        @DisplayName("Deve criar livro com sucesso")
        void shouldCreateBook() {
            // Given
            when(repository.findByIsbn(createDTO.getIsbn())).thenReturn(Optional.empty());
            when(mapper.toEntity(createDTO)).thenReturn(book);
            when(repository.save(book)).thenReturn(book);
            when(mapper.toResponse(book)).thenReturn(responseDTO);

            // When
            BookResponseDTO result = bookService.create(createDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsbn()).isEqualTo(createDTO.getIsbn());
            verify(repository).save(book);
        }

        @Test
        @DisplayName("Deve lançar exceção quando ISBN já existir")
        void shouldThrowWhenIsbnExists() {
            // Given
            when(repository.findByIsbn(createDTO.getIsbn())).thenReturn(Optional.of(book));

            // When / Then
            assertThatThrownBy(() -> bookService.create(createDTO))
                    .isInstanceOf(BookAlreadyExistsException.class)
                    .hasMessage("ISBN already exists");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class QueryTests {

        @Test
        @DisplayName("Deve retornar página de livros")
        void shouldReturnBooksPage() {
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(book)));
            when(mapper.toResponse(book)).thenReturn(responseDTO);

            Page<BookResponseDTO> result = bookService.getAllPage(pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Deve retornar vazio quando não houver livros")
        void shouldReturnEmptyPage() {
            when(repository.findAll(pageable)).thenReturn(Page.empty());

            Page<BookResponseDTO> result = bookService.getAllPage(pageable);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve buscar livros por autor")
        void shouldSearchByAuthor() {
            when(repository.findByAuthorContainingIgnoreCase("Brendan", pageable))
                    .thenReturn(new PageImpl<>(List.of(book)));
            when(mapper.toResponse(book)).thenReturn(responseDTO);

            Page<BookResponseDTO> result = bookService.searchByAuthor("Brendan", pageable);

            assertThat(result.getContent().getFirst().getAuthor()).isEqualTo(book.getAuthor());
        }

        @Test
        @DisplayName("Deve retornar vazio quando autor não encontrado")
        void shouldReturnEmptyWhenAuthorNotFound() {
            when(repository.findByAuthorContainingIgnoreCase("x", pageable))
                    .thenReturn(Page.empty());

            Page<BookResponseDTO> result = bookService.searchByAuthor("x", pageable);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve buscar livros por título")
        void shouldSearchByTitle() {
            when(repository.findByTitleContainingIgnoreCase("Kubernetes", pageable))
                    .thenReturn(new PageImpl<>(List.of(book)));
            when(mapper.toResponse(book)).thenReturn(responseDTO);

            Page<BookResponseDTO> result = bookService.searchByTitle("Kubernetes", pageable);

            assertThat(result.getContent().getFirst().getTitle()).isEqualTo(book.getTitle());
        }

        @Test
        @DisplayName("Deve retornar vazio quando título não encontrado")
        void shouldReturnEmptyWhenTitleNotFound() {
            when(repository.findByTitleContainingIgnoreCase("x", pageable))
                    .thenReturn(Page.empty());

            Page<BookResponseDTO> result = bookService.searchByTitle("x", pageable);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve filtrar por faixa de preço")
        void shouldFilterByPrice() {
            when(repository.findByPriceBetween(100.0, 200.0, pageable))
                    .thenReturn(new PageImpl<>(List.of(book)));
            when(mapper.toResponse(book)).thenReturn(responseDTO);

            Page<BookResponseDTO> result = bookService.filterByPrice(100.0, 200.0, pageable);

            assertThat(result.getContent().getFirst().getPrice())
                    .isEqualByComparingTo(new BigDecimal("180.00"));
        }

        @Test
        @DisplayName("Deve retornar vazio quando não houver livros na faixa de preço")
        void shouldReturnEmptyWhenPriceRange() {
            when(repository.findByPriceBetween(500.0, 1000.0, pageable))
                    .thenReturn(Page.empty());

            Page<BookResponseDTO> result = bookService.filterByPrice(500.0, 1000.0, pageable);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Controle de estoque")
    class StockTests {

        @Test
        @DisplayName("Deve diminuir o estoque corretamente")
        void shouldDecreaseStock() {
            // Given
            when(repository.findById(book.getId())).thenReturn(Optional.of(book));

            // When
            bookService.decreaseStock(book.getId().toString(), 5);

            // Then
            assertThat(book.getStock()).isEqualTo(10);
            verify(repository).save(book);
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro não existir")
        void shouldThrowWhenBookNotFound() {
            // Given
            when(repository.findById(book.getId())).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.decreaseStock(book.getId().toString(), 5))
                    .isInstanceOf(BookNotExistsException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção quando estoque insuficiente")
        void shouldThrowWhenStockInsufficient() {
            // Given
            when(repository.findById(book.getId())).thenReturn(Optional.of(book));

            // When / Then
            assertThatThrownBy(() -> bookService.decreaseStock(book.getId().toString(), 99))
                    .isInstanceOf(InsufficientStockException.class);
        }
    }

    @Nested
    @DisplayName("Validação de livros (eventos)")
    class ValidationTests {

        @Test
        @DisplayName("Deve publicar evento com available=true")
        void shouldPublishAvailableTrue() {
            // Given
            BookValidationRequest request = new BookValidationRequest();
            request.setOrderId(UUID.randomUUID());
            request.setBookId(book.getId());
            request.setQuantity(5);

            when(repository.findById(book.getId())).thenReturn(Optional.of(book));

            // When
            bookService.processValidation(request);

            // Then
            ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
            verify(processedEventRepository).saveAndFlush(captor.capture());

            assertThat(captor.getValue().getId())
                    .contains(request.getOrderId().toString());

            verify(rabbitTemplate).convertAndSend(
                    eq("book.events"),
                    eq("book.validated"),
                    any(BookValidatedEvent.class)
            );

            verify(eventMetrics).published("book_validated");
            verify(metrics).validated();
            verify(metrics, never()).cancelled(anyString());
        }

        @Test
        @DisplayName("Deve publicar evento com available=false")
        void shouldPublishAvailableFalse() {
            // Given
            BookValidationRequest request = new BookValidationRequest();
            request.setOrderId(UUID.randomUUID());
            request.setBookId(book.getId());
            request.setQuantity(99);

            when(repository.findById(book.getId())).thenReturn(Optional.of(book));

            // When
            bookService.processValidation(request);

            // Then
            ArgumentCaptor<BookValidatedEvent> captor = ArgumentCaptor.forClass(BookValidatedEvent.class);
            verify(rabbitTemplate).convertAndSend(eq("book.events"), eq("book.validated"), captor.capture());

            assertThat(captor.getValue().isAvailable()).isFalse();

            verify(eventMetrics).published("book_validated");
            verify(metrics).cancelled("book_unavailable");
            verify(metrics, never()).validated();
        }

        @Test
        @DisplayName("Deve lançar exceção quando livro não existir na validação")
        void shouldThrowWhenValidationBookNotFound() {
            // Given
            BookValidationRequest request = new BookValidationRequest();
            request.setOrderId(UUID.randomUUID());
            request.setBookId(UUID.randomUUID());
            request.setQuantity(5);

            when(repository.findById(request.getBookId())).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.processValidation(request))
                    .isInstanceOf(BookNotExistsException.class)
                    .hasMessage("Book not found");

            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), Optional.ofNullable(any()));
            verify(eventMetrics, never()).published(anyString());
            verify(metrics, never()).validated();
            verify(metrics, never()).cancelled(anyString());
        }

        @Test
        @DisplayName("Deve publicar evento com available=true e dados corretos")
        void shouldPublishAvailableTrueWithData() {
            // Given
            BookValidationRequest request = new BookValidationRequest();
            request.setOrderId(UUID.randomUUID());
            request.setBookId(book.getId());
            request.setQuantity(5);

            when(repository.findById(book.getId())).thenReturn(Optional.of(book));

            // When
            bookService.processValidation(request);

            // Then
            ArgumentCaptor<BookValidatedEvent> captor = ArgumentCaptor.forClass(BookValidatedEvent.class);
            verify(rabbitTemplate).convertAndSend(eq("book.events"), eq("book.validated"), captor.capture());

            BookValidatedEvent event = captor.getValue();

            assertThat(event.getOrderId()).isEqualTo(request.getOrderId());
            assertThat(event.getIsbn()).isEqualTo(book.getIsbn());
            assertThat(event.getBookId()).isEqualTo(book.getId());
            assertThat(event.getQuantity()).isEqualTo(5);
            assertThat(event.getPrice()).isEqualByComparingTo(book.getPrice());
            assertThat(event.isAvailable()).isTrue();

            verify(eventMetrics).published("book_validated");
            verify(metrics).validated();
        }

        @Test
        @DisplayName("Deve seguir ordem correta: salvar evento antes de buscar livro")
        void shouldRespectExecutionOrder() {
            // Given
            BookValidationRequest request = new BookValidationRequest();
            request.setOrderId(UUID.randomUUID());
            request.setBookId(book.getId());
            request.setQuantity(5);

            when(repository.findById(book.getId())).thenReturn(Optional.of(book));

            // When
            bookService.processValidation(request);

            // Then
            InOrder inOrder = inOrder(processedEventRepository, repository);

            inOrder.verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
            inOrder.verify(repository).findById(any());
        }

        @Test
        @DisplayName("Não deve processar evento duplicado (idempotência)")
        void shouldIgnoreDuplicateEvent() {
            // Given
            BookValidationRequest request = new BookValidationRequest();
            request.setOrderId(UUID.randomUUID());
            request.setBookId(book.getId());
            request.setQuantity(5);

            when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicated"));

            // When
            bookService.processValidation(request);

            // Then
            verify(repository, never()).findById(any());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), Optional.ofNullable(any()));
            verify(eventMetrics, never()).published(anyString());
            verify(metrics, never()).validated();
            verify(metrics, never()).cancelled(anyString());
        }
    }

    @Nested
    @DisplayName("Baixa de estoque via evento")
    class StockDecreaseTests {

        @Test
        @DisplayName("Deve diminuir estoque quando evento for válido")
        void shouldDecreaseStockWhenEventIsValid() {
            // Given
            UUID eventId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();

            StockDecreaseEvent event = new StockDecreaseEvent(eventId, orderId, bookId, 3);

            Book book = new Book();
            book.setId(bookId);
            book.setStock(10);

            when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                    .thenReturn(new ProcessedEvent(eventId.toString()));

            when(repository.findById(bookId))
                    .thenReturn(Optional.of(book));

            // When
            bookService.processStockDecrease(event);

            // Then
            assertThat(book.getStock()).isEqualTo(7);

            verify(repository).save(book);
            verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
            verify(metrics).stockSuccess();
            verify(metrics, never()).stockError(anyString());
            verify(eventMetrics, never()).duplicated(anyString());
        }

        @Test
        @DisplayName("Deve ignorar evento duplicado")
        void shouldIgnoreDuplicatedStockDecreaseEvent() {
            // Given
            UUID eventId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();

            StockDecreaseEvent event = new StockDecreaseEvent(eventId, orderId, bookId, 3);

            when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicated"));

            // When
            bookService.processStockDecrease(event);

            // Then
            verify(repository, never()).findById(any());
            verify(repository, never()).save(any());
            verify(eventMetrics).duplicated("stock_decrease");
            verify(metrics, never()).stockSuccess();
            verify(metrics, never()).stockError(anyString());
        }

        @Test
        @DisplayName("Deve lançar erro quando livro não existir")
        void shouldThrowWhenBookDoesNotExist() {
            // Given
            UUID eventId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();

            StockDecreaseEvent event = new StockDecreaseEvent(eventId, orderId, bookId, 3);

            when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                    .thenReturn(new ProcessedEvent(eventId.toString()));

            when(repository.findById(bookId))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> bookService.processStockDecrease(event))
                    .isInstanceOf(BookNotExistsException.class)
                    .hasMessage("Book not found");

            verify(repository, never()).save(any());
            verify(metrics, never()).stockSuccess();
            verify(metrics, never()).stockError(anyString());
        }

        @Test
        @DisplayName("Deve lançar erro quando estoque for insuficiente")
        void shouldThrowWhenStockIsInsufficient() {
            // Given
            UUID eventId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();

            StockDecreaseEvent event = new StockDecreaseEvent(eventId, orderId, bookId, 10);

            Book book = new Book();
            book.setId(bookId);
            book.setStock(3);

            when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                    .thenReturn(new ProcessedEvent(eventId.toString()));

            when(repository.findById(bookId))
                    .thenReturn(Optional.of(book));

            // When / Then
            assertThatThrownBy(() -> bookService.processStockDecrease(event))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Insufficient stock");

            assertThat(book.getStock()).isEqualTo(3);

            verify(repository, never()).save(any());
            verify(metrics).stockError("insufficient_stock");
            verify(metrics, never()).stockSuccess();
        }
    }
}