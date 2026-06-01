package common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  private Long id;
  private Long bidderId;
  private Long auctionId;
  private Double amount;
  private LocalDateTime bidTime;
}
