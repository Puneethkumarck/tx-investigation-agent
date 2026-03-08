package com.stablebridge.txinvestigation.domain.model;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String service, String message) {
        super("Service unavailable [%s]: %s".formatted(service, message));
    }
}
