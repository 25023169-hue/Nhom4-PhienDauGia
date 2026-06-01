package common.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
