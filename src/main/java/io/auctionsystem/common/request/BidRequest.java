package io.auctionsystem.common.request;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest implements Serializable {
  private static final long serialVersionUID = 1L;
  private Long auctionId;
  private Long bidderId;
  private Double amount;
}
