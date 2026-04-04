package com.scarlxrd.catalog_service.config.rabbitmq;

import com.scarlxrd.catalog_service.dto.BookValidationRequest;
import com.scarlxrd.catalog_service.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatalogConsumer {

    private final BookService service;

    @RabbitListener(queues = "book.validate.queue")
    public void handle(BookValidationRequest request){

        service.processValidation(request);
    }

}
