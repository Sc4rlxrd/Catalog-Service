package com.scarlxrd.catalog_service.config.rabbitmq;

import com.scarlxrd.catalog_service.dto.BookValidationRequest;
import com.scarlxrd.catalog_service.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class CatalogConsumer {

    private final BookService service;

    @RabbitListener(
            queues = "book.validate.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handle(BookValidationRequest request){

        log.info("Event received: {}",request);
        service.processValidation(request);
    }

}
