package com.scarlxrd.catalog_service.dto;


import java.util.UUID;

public record StockDecreaseEvent(
        UUID eventId,
        UUID orderId,
        UUID bookId,
        int quantity
) {
}
