package client.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import client.ServerConnectionException;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class ClientHttp {
  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private ClientHttp() {}

  public static HttpResponse<String> send(HttpRequest request) {
    try {
      return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServerConnectionException(e);
    } catch (IOException e) {
      throw new ServerConnectionException(e);
    }
  }

  public static ObjectMapper mapper() {
    return OBJECT_MAPPER;
  }
}
