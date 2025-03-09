package com.example.carpark.exception;

public class CarParkException extends RuntimeException {
    public CarParkException(String message) {
        super(message);
    }

    public CarParkException(String message, Throwable cause) {
        super(message, cause);
    }
}
