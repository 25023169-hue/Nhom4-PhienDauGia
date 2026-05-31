package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "bidders")
@Inheritance(strategy = InheritanceType.JOINED)
public class Bidder extends User {
  private String bankName;
  private String accountName;
  private String bankAccount;
  private String address;
}
