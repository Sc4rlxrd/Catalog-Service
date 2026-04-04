package com.scarlxrd.catalog_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class BookValidationRequest {

    private UUID orderId;
    private String isbn;
    private int quantity;

}


