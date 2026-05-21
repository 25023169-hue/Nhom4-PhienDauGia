package io.auctionsystem.server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fashion_items")
public class Fashion extends Item {
    private String brand;
    private String size;
    private String material;
    private String gender; // Nam, Nữ, Unisex
}