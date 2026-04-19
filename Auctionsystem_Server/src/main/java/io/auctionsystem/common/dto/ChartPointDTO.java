package io.auctionsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChartPointDTO {
    private String timestamp; // Nhãn trục X (Thời gian đặt giá)
    private Double price;     // Nhãn trục Y (Giá tiền)
}
