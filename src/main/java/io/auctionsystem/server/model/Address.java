package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "addresses")
public class Address extends BaseEntity {
    private Long userId;
    private String receiverName;
    private String phoneNumber;
    private String street;
    private String city;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}