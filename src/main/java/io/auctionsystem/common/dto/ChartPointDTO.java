package io.auctionsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartPointDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String timestamp;
    private Double price;
}