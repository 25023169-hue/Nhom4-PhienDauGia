package io.auctionsystem.common.dto;

import io.auctionsystem.common.enums.AuctionState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long auctionId;
    private String itemName;
    private Double currentPrice;
    private AuctionState status;
    private String endTime;
}