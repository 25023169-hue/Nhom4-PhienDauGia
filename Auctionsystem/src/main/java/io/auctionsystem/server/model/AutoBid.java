package io.auctionsystem.server.model;

import jakarta.persistence.*; // Bắt buộc phải import cái này
import lombok.*;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity  // Thêm dòng này để Spring biết đây là một bảng
@Table(name = "auto_bids") // Thêm dòng này để đặt tên bảng
public class AutoBid extends BaseEntity {
    private Long auctionId;
    private Long bidderId;
    private Double maxLimit;
    private Double stepAmount;
    private Boolean isActive;
}