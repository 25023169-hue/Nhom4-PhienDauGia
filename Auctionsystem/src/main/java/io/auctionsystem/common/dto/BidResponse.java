package io.auctionsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private boolean isAccepted;
    private String message;
    private Long auctionId;
    private Double newCurrentPrice;
    private String newWinnerName;
}
