package io.auctionsystem.common.dto;

import io.auctionsystem.common.enums.AuctionState;
import lombok.Data;

@Data
public class AuctionDTO {
    private Long auctionId;
    private String itemName;
    private Double currentPrice;
    private AuctionState status;
    private String endTime;
}
