package io.auctionsystem.common.dto;

import lombok.Data;

@Data
public class AutoBidRequest {
    private Long auctionId;
    private Long bidderId;
    private Double maxLimit; // Mức giá tối đa chịu chi
    private Double stepAmount; // Mỗi lần nhảy giá bao nhiêu
}