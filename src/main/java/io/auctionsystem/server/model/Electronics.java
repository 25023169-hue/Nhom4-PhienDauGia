package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "electronics_items")
public class Electronics extends Item {
    private String brand;
    private String model;
    private Integer condition;
}