package io.auctionsystem.server.model;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.ItemType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private Double bidIncrement;
    private Double buyNowPrice;
    private String imageUrl;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    @Enumerated(EnumType.STRING)
    private AuctionState status;
}
