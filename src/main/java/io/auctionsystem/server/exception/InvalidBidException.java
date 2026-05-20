package io.auctionsystem.server.exception;

public class InvalidBidException extends IllegalArgumentException {
    public InvalidBidException(String message) {
        super(message);
    }
}