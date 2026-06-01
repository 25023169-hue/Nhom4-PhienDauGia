package common;

public class Constants {
  // Địa chỉ gốc của Server
  public static final String BASE_URL = "http://localhost:8080/api";
  public static final String SOCKET_URL = "ws://localhost:8080/ws";

  // Các cổng (Endpoints) để App gọi dữ liệu
  public static final String LOGIN_ENDPOINT = BASE_URL + "/auth/login";
  public static final String AUCTION_ENDPOINT = BASE_URL + "/auctions";

  // Các "kênh" (Topics) để App lắng nghe dữ liệu thời gian thực qua Socket
  public static final String TOPIC_BIDS = "/topic/bids";
  public static final String TOPIC_AUCTIONS = "/topic/auctions";
  public static final String TOPIC_NOTIFICATIONS = "/topic/notifications";
  public static final String TOPIC_CHART = "/topic/chart";
}
