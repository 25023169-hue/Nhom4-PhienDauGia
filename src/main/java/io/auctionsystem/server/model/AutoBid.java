package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "auto_bids")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "bidder_id", nullable = false)
    private Long bidderId;

    @Column(name = "max_amount", nullable = false)
    private Double maxAmount;       // Mức giá trần tối đa người dùng có thể chi trả

    @Column(name = "increment_amount", nullable = false)
    private Double incrementAmount; // Bước giá tự động cộng thêm khi có người đè giá

    @Column(name = "active", nullable = false)
    private boolean active = true;  // Trạng thái bật/tắt hoạt động của robot Auto-Bid
}