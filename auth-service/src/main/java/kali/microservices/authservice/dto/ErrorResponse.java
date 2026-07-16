package kali.microservices.authservice.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ErrorResponse {
    private String errorCode;
    private String message;
    private int statusCode;
    private Instant timestamp;

    public ErrorResponse(String errorCode, String message, int statusCode) {
        this.errorCode = errorCode;
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = Instant.now();
    }
}