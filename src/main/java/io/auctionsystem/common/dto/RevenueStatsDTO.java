package io.auctionsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Double totalRevenue;
    private Double monthRevenue;
    private Long soldOrders;
    private Double averageOrderValue;
    private List<ChartPointDTO> monthlyRevenue;
    private List<TransactionDTO> recentSales;
}
