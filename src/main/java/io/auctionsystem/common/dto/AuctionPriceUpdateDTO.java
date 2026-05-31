package io.auctionsystem.common.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionPriceUpdateDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  private Long auctionId;
  private Double currentPrice;
}
