package com.scarlxrd.catalog_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookValidationRequest {

    private UUID orderId;
    private UUID bookId;
    private int quantity;

}


