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
@Table(name = "jewelry_items")
public class Jewelry extends Item {
    private String material; // Vàng, Bạc, Kim cương...
    private Double weight;   // Trọng lượng
    private String gemstone; // Đá quý đính kèm
}