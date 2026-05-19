package io.auctionsystem.common.response;

import io.auctionsystem.common.enums.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String firstname;
    private String lastname;
    private double balance;
    private Role role;
    private String address;
    private String bankName;
    private String accountName;
    private String bankAccount;
    private String storeName;
}