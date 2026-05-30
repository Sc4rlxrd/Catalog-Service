package com.scarlxrd.catalog_service.config.rabbitmq;

import com.scarlxrd.catalog_service.dto.StockDecreaseEvent;
import com.scarlxrd.catalog_service.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockDecreaseConsumer {

    private final BookService bookService;

    @RabbitListener(queues = "stock.decrease.queue")
    public void consume(StockDecreaseEvent event) {

        log.info("CONSUMER RECEIVED: {}", event);

        bookService.processStockDecrease(event);
    }
}
