package com.scarlxrd.catalog_service.exception;

public class BookAlreadyExistsException extends BusinessException {
    public BookAlreadyExistsException(String message) {
        super(message);
    }
}
