package io.auctionsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "items")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Item extends BaseEntity {
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private double startingPrice;
    private double currentPrice;
    private Long sellerId;
}
