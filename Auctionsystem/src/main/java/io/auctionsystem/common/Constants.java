package io.auctionsystem.common;

public class Constants {
    public static final String BASE_URL = "http://localhost:8080/api";
    public static final String SOCKET_URL = "ws://localhost:8080/ws";

    public static final String LOGIN_ENDPOINT = BASE_URL + "/auth/login";
    public static final String AUCTION_ENDPOINT = BASE_URL + "/auctions";

    public static final String TOPIC_BIDS = "/topic/bids";
}
