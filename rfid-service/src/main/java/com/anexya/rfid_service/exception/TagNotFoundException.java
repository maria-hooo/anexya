package com.anexya.rfid_service.exception;

public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException(Long id) {
        super("RFID tag entry with id " + id + " was not found");
    }
}
