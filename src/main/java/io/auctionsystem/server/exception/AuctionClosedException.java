package io.auctionsystem.server.exception;

public class AuctionClosedException extends IllegalArgumentException {
    public AuctionClosedException(String message) {
        super(message);
    }
}