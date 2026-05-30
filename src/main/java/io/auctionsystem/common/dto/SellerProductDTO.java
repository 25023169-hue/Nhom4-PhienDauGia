package io.auctionsystem.common.dto;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long listingId;
    private Long itemId;
    private Long sellerId;
    private String itemName;
    private String description;
    private ItemType itemType;
    private Double startingPrice;
    private Double currentPrice;
    private Double soldPrice;
    private Double buyNowPrice;
    private String imageUrl;
    private String startTime;
    private String endTime;
    private AuctionState status;
    private boolean editable;
}
