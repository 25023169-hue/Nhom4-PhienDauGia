package server.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void handleNotFound_Returns404() {
    var response = handler.handleNotFound(new ResourceNotFoundException("missing"));

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("missing", ((Map<?, ?>) response.getBody()).get("message"));
  }

  @Test
  void handleBadRequest_Returns400() {
    var response = handler.handleBadRequest(new ValidationException("invalid"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("invalid", ((Map<?, ?>) response.getBody()).get("message"));
  }

  @Test
  void handleAllExceptions_Returns500() {
    var response = handler.handleAllExceptions(new RuntimeException("internal"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("Lỗi Server", ((Map<?, ?>) response.getBody()).get("message"));
  }
}
