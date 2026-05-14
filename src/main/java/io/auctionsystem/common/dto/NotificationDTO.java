package io.auctionsystem.common.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
    private String type;
}