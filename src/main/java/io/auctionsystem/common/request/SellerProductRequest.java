package io.auctionsystem.common.request;

import io.auctionsystem.common.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerProductRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long sellerId;
    private String name;
    private String description;
    private Double startingPrice;
    private Double buyNowPrice;
    private String imageUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ItemType itemType;

    private String artistName;
    private String medium;
    private String dimensions;
    private Integer creationYear;

    private String brand;
    private String model;
    private Integer warrantyMonths;

    private String vinCode;
    private Integer manufactureYear;
    private String fuelType;

    private String size;
    private String material;
    private String gender;

    private Double weight;
    private String gemstone;
}
