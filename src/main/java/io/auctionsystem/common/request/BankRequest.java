package io.auctionsystem.common.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankRequest {
  private String bankName;
  private String accountName;
  private String bankAccount;
}
