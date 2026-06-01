package server.model;

import common.enums.AuctionState;
import common.enums.ItemType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "seller_product_listings")
public class SellerProductListing extends BaseEntity {
  private Long itemId;
  private Long sellerId;
  private Double buyNowPrice;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @Enumerated(EnumType.STRING)
  private ItemType itemType;

  @Enumerated(EnumType.STRING)
  private AuctionState status;

  private boolean hidden;
}
