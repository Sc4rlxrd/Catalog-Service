package com.scarlxrd.catalog_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class BookValidationRequest {

    private UUID orderId;
    private UUID bookId;
    private int quantity;

}


