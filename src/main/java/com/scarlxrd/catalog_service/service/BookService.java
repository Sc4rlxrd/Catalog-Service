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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class BookService {

    private  final BookRepository repository;
    private final BookMapper mapper;
    private final RabbitTemplate rabbitTemplate;
    private final ProcessedEventRepository processedEventRepository;
    private final CatalogMetrics metrics;
    private final RabbitEventMetrics eventMetrics;

    @Transactional
    public BookResponseDTO create(CreateBookDTO dto){

        repository.findByIsbn(dto.getIsbn()).ifPresent(b ->{
            throw new BookAlreadyExistsException("ISBN already exists");
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
                .orElseThrow(() -> new BookNotExistsException("Book not found"));

        if(book.getStock() < quantity){
            throw new InsufficientStockException("Insufficient stock");
        }

        book.setStock(book.getStock() - quantity);
        repository.save(book);
    }

    // FOR RABBITMQ
    @Transactional
    public void processValidation(BookValidationRequest request) {

        String eventId = request.getOrderId() + "-" + request.getBookId();

        if (isDuplicate(eventId)) {
            log.warn("Duplicate event detected at DB level: {}", eventId);
            return;
        }

        Book book = repository.findById(request.getBookId())
                .orElseThrow(() -> new BookNotExistsException("Book not found"));

        boolean available = book.getStock() >= request.getQuantity();

        log.info("Creating a validation event for an order: {}, book: {}", request.getOrderId(), request.getBookId());

        BookValidatedEvent event = new BookValidatedEvent(
                request.getOrderId(),
                book.getIsbn(),
                book.getId(),
                book.getPrice(),
                available,
                request.getQuantity()
        );

        log.info("Sending event to RabbitMQ: {}", event);

        rabbitTemplate.convertAndSend(
                "book.events",
                "book.validated",
                event
        );
        eventMetrics.published("book_validated");
        if (available) {
            metrics.validated();
        } else {
            metrics.cancelled("book_unavailable");
        }
    }

    // FOR RABBITMQ
    @Transactional
    public void processStockDecrease(StockDecreaseEvent event) {

        log.info("EVENT RECEIVED: {}", event);

        if (isDuplicate(event.eventId().toString())) {
            log.warn("Duplicate stock event detected: {}", event.eventId());
            eventMetrics.duplicated("stock_decrease");
            return;
        }

        Book book = repository.findById(event.bookId())
                .orElseThrow(() -> new BookNotExistsException("Book not found"));

        if (book.getStock() < event.quantity()) {
            metrics.stockError("insufficient_stock");
            throw new InsufficientStockException("Insufficient stock");
        }

        book.setStock(book.getStock() - event.quantity());
        repository.save(book);
        metrics.stockSuccess();

        log.info(
                "Stock decreased for book {} by {} units",
                book.getId(),
                event.quantity()
        );
    }

    private boolean isDuplicate(String eventId) {
        try {
            processedEventRepository.saveAndFlush(new ProcessedEvent(eventId));
            return false;
        } catch (DataIntegrityViolationException e) {
            return true;
        }
    }
}
