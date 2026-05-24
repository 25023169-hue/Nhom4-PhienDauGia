package io.auctionsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * FILE MỚI - Cần tạo file này trong module common.
 *
 * Lý do: PurchaseHistoryController (client) trước đây import
 * io.auctionsystem.server.model.Bid (server model) → sai kiến trúc,
 * client không được phép phụ thuộc vào server model.
 *
 * Giải pháp: Tạo BidDTO trong common module để cả client và server đều dùng được.
 * Server endpoint /api/bids/history/{bidderId} cần serialize ra JSON tương thích với class này.
 */
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