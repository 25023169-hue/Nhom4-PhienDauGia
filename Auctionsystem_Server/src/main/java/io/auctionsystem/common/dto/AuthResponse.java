package io.auctionsystem.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token; // Token nếu bạn dùng JWT
    private Long userId;
    private String username;
    private String role;
}