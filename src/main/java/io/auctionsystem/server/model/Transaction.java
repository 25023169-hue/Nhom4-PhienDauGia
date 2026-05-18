package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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
    private String type;
    private String note;
    private LocalDateTime transactionTime;
}