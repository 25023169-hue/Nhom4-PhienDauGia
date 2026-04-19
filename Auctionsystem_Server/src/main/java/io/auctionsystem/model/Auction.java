package io.auctionsystem.model;

import io.auctionsystem.common.enums.AuctionState;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
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

