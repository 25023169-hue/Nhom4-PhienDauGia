package server.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User extends BaseEntity {
  private double balance = 0.0;
  private double heldBalance = 0.0;
  private String username;
  private String password;
  private String firstname;
  private String lastname;

  // LỖI ĐÃ SỬA: bankName và bankAccount bị trùng lặp với Bidder
  // Trong Bidder đã khai báo: private String bankName, accountName, bankAccount, address
  // → Xóa bankName và bankAccount khỏi User để tránh shadow field và lỗi Hibernate mapping.
  // Nếu cần dùng bankName/bankAccount ở tầng User, hãy lấy qua ép kiểu sang Bidder.

  private boolean isBanned = false;
  private boolean active = true;

  public double getAvailableBalance() {
    return balance - heldBalance;
  }
}
