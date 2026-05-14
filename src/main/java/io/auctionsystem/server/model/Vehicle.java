package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "vehicles")
public class Vehicle extends Item {
    private String vinCode;
    private Integer manufactureYear;
    private String fuelType;
}