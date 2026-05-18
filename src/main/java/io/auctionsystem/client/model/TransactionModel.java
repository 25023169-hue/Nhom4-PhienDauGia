package io.auctionsystem.client.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TransactionModel {
    private final LocalDateTime rawTime;
    private final String time;
    private final String moneyIn;
    private final String moneyOut;
    private final String lastBalance;
    private final String type;
    private final String note;
}