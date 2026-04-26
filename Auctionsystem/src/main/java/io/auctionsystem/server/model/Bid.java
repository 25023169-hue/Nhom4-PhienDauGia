package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "bids")
public class Bid extends BaseEntity {
    private Long bidderId;
    private Long auctionId;
    private Double amount;
    private LocalDateTime bidTime;
}