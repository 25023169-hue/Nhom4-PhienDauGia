package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Người nhận thông báo

    private String message;
    private String type;
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean isRead = false;

    public Notification(User user, String message, String type) {
        this.user = user;
        this.message = message;
        this.type = type;
    }
}