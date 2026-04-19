package org.example.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Auction extends Entity {
    private int itemId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer winnerId;
    private Double finalPrice;
}