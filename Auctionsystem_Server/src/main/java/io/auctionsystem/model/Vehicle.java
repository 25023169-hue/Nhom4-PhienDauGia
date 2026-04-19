package io.auctionsystem.model;

import jakarta.persistence.*;
import lombok.*;

@Data
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