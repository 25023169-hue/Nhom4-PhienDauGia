package server.model;

import common.enums.AuctionState;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "auctions")
public class Auction extends BaseEntity {
  private Long itemId;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Long winnerId;
  private Double finalPrice;

  @Enumerated(EnumType.STRING)
  private AuctionState status;
}
