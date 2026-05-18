package io.auctionsystem.server.model;

import jakarta.persistence.*; // Bắt buộc phải import cái này
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "auto_bids")
public class AutoBid extends BaseEntity {
    private Long auctionId;
    private Long bidderId;
    private Double maxLimit;
    private Double stepAmount;
    private Boolean isActive;
}