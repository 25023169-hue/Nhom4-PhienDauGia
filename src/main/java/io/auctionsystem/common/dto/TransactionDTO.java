package io.auctionsystem.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO implements Serializable {
  private static final long serialVersionUID = 1L;
  private Long id;
  private Long userId;
  private Double moneyIn;
  private Double moneyOut;
  private Double lastBalance;
  private String type;
  private String note;
  private LocalDateTime transactionTime;
  private LocalDateTime createdAt;
}
