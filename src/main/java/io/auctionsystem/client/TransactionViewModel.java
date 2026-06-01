package io.auctionsystem.client;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionViewModel {
  private final Long id;
  private final LocalDateTime rawTime;
  private final String time;
  private final String moneyIn;
  private final String moneyOut;
  private final String lastBalance;
  private final String type;
  private final String note;
}
