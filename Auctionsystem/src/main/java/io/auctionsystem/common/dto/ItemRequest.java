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


    private String artist;
    private String brand;
    private Integer manufactureYear;
}
