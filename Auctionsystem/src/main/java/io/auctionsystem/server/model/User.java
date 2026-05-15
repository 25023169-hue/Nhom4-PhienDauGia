package io.auctionsystem.server.model;

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
    private String username;
    private String password;
    private String firstname;
    private String lastname;
    private String bankName;
    private int bankAccount;
    // Thêm vào dưới thuộc tính bankAccount
    private boolean isBanned = false;

// Nhớ thêm các annotation của Lombok nếu chưa có,
// hoặc tự generate Getter/Setter cho thuộc tính này nhé.
}