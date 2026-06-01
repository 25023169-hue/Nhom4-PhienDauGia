package server.exception;

public class AuctionClosedException extends BusinessException {
  public AuctionClosedException(String message) {
    super(message);
  }
}
