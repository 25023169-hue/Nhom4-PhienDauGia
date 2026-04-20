package io.auctionsystem.common.dto;

import lombok.Data;

@Data
public class AutoBidRequest {
    private Long auctionId;
    private Long bidderId;
    private Double maxLimit;
    private Double stepAmount;
}