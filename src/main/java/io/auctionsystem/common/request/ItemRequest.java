package io.auctionsystem.common.request;

import io.auctionsystem.common.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private Double startingPrice;
    private Long sellerId;
    private ItemType type;

    // Các trường đặc thù
    private String artist;
    private String brand;
    private Integer manufactureYear;
    // --- CÁC TRƯỜNG THÊM MỚI CHO FASHION & JEWELRY ---
    private String size;
    private String material;
    private String gender;
    private Double weight;
    private String gemstone;
}