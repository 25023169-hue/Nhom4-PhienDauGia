package io.auctionsystem.common.response;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse implements Serializable {
  private static final long serialVersionUID = 1L;
  private boolean isAccepted;
  private String message;
  private Long auctionId;
  private Double newCurrentPrice;
  private String newWinnerName;
  private Long previousWinnerId;
}
