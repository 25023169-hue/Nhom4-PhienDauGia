package io.auctionsystem.common.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class BankRequest {
    private String bankName;
    private String accountName;
    private String bankAccount;
}