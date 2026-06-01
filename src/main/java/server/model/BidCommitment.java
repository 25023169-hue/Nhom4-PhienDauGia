package server.model;

import common.enums.BidCommitmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
    name = "bid_commitments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"auction_id", "bidder_id"}))
public class BidCommitment extends BaseEntity {
  @Column(name = "auction_id", nullable = false)
  private Long auctionId;

  @Column(name = "bidder_id", nullable = false)
  private Long bidderId;

  @Column(nullable = false)
  private Double amount = 0.0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BidCommitmentStatus status = BidCommitmentStatus.ACTIVE;
}
