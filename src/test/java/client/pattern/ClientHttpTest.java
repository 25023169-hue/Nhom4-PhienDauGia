package client.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClientHttpTest {

  @Test
  void extractMessage_JsonError_ReturnsBusinessMessageOnly() {
    String body =
        """
        {"timestamp":"2026-06-02T00:00:00Z","status":400,
        "message":"Tài khoản hoặc mật khẩu không chính xác!"}
        """;

    assertEquals(
        "Tài khoản hoặc mật khẩu không chính xác!",
        ClientHttp.extractMessage(body, "fallback"));
  }

  @Test
  void extractMessage_PlainText_ReturnsOriginalText() {
    assertEquals("Lỗi cũ", ClientHttp.extractMessage("Lỗi cũ", "fallback"));
  }

  @Test
  void extractMessage_EmptyBody_ReturnsFallback() {
    assertEquals("fallback", ClientHttp.extractMessage(" ", "fallback"));
  }
}
