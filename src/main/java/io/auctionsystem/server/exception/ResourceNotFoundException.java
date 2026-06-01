package io.auctionsystem.server.exception;

public class ResourceNotFoundException extends BusinessException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
