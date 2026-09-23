package com.rpe.catalog.service.exception;

public class DuplicateProductNameException extends RuntimeException {

    public DuplicateProductNameException(String name) {
        super("A product named '" + name + "' already exists");
    }
}
