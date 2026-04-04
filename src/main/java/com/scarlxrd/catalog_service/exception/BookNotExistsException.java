package com.scarlxrd.catalog_service.exception;

public class BookNotExistsException extends BusinessException {
    public BookNotExistsException(String message) {
        super(message);
    }
}
