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
    private String accountName;
    private String bankAccount;
    private String address;
}