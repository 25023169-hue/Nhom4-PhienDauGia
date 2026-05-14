package io.auctionsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private Double currentPrice;
    private String endTime;
    private String status;
}