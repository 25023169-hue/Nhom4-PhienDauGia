package server.model;

import common.enums.TransactionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import server.model.converter.TransactionTypeConverter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {
  private Long userId;
  private Double moneyIn = 0.0;
  private Double moneyOut = 0.0;
  private Double lastBalance;
  @Convert(converter = TransactionTypeConverter.class)
  private TransactionType type;
  private String note;
  private LocalDateTime transactionTime;
}
