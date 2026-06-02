package server.exception;

public class ServerConnectionException extends RuntimeException {
  public static final String MESSAGE = "Lỗi kết nối server!";

  public ServerConnectionException(Throwable cause) {
    super(MESSAGE, cause);
  }
}
