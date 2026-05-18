package io.auctionsystem.common.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoBidRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long auctionId;
    private Long bidderId;
    private Double maxLimit;
    private Double stepAmount;
}