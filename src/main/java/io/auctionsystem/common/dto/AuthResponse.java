package io.auctionsystem.common.dto;

import io.auctionsystem.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String token;
    private Long id;
    private String username;
    private String firstname;
    private String lastname;
    private double balance;
    private String bankName;
    private String bankAccount;
    private Role role;
}
