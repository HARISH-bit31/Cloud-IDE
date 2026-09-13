package com.cloudide.cloudide.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class QueueFullException extends RuntimeException {
    public QueueFullException(String message) {
        super(message);
    }
}
