package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BidTransaction extends Entity {
    private int auctionId;
    private int bidderId;
    private double bidAmount;
    private LocalDateTime bidTime;
}