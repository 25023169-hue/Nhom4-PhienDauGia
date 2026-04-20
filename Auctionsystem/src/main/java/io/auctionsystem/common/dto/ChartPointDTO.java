package io.auctionsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChartPointDTO {
    private String timestamp;
    private Double price;
}
