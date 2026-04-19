package io.auctionsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {
    private Long auctionId;
    private Long bidderId;
    private Double amount;
}
