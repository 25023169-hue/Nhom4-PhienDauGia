package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "vehicles")
//@DiscriminatorValue("Vehicle")
public class Vehicle extends Item {
    private String vinCode;
    private Integer manufactureYear;
    private String fuelType;
}