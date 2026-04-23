package io.auctionsystem.common.dto;

public class AuctionItemDTO {
    private Long id;
    private String name;
    private Double currentPrice;
    private String endTime;
    private String status;

    // Constructor, Getter và Setter (Quan trọng: Phải có Getter thì TableView mới lấy được dữ liệu)
    public AuctionItemDTO(Long id, String name, Double currentPrice, String endTime, String status) {
        this.id = id;
        this.name = name;
        this.currentPrice = currentPrice;
        this.endTime = endTime;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Double getCurrentPrice() { return currentPrice; }
    public String getEndTime() { return endTime; }
    public String getStatus() { return status; }
}