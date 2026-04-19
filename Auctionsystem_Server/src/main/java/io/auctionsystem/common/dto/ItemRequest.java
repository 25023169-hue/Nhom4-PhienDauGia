package io.auctionsystem.common.dto;

import io.auctionsystem.common.enums.ItemType;
import lombok.Data;

@Data
public class ItemRequest {
    private String name;
    private String description;
    private Double startingPrice;
    private Long sellerId;
    private ItemType type;

    // Các trường phụ thuộc vào loại Item (Có thể null)
    private String artist;          // Cho Art
    private String brand;           // Cho Electronics
    private Integer manufactureYear; // Cho Vehicle
}
