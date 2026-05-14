package io.auctionsystem.common.dto;

import io.auctionsystem.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String firstname;
    private String lastname;
    private Role role;
}