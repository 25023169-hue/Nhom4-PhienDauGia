package server.exception;

public abstract class BusinessException extends IllegalArgumentException {
  protected BusinessException(String message) {
    super(message);
  }
}
