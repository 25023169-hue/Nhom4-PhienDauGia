package io.auctionsystem.server.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
 @Table(name = "electronic_items")
//@DiscriminatorValue("ELECTRONICS")
public class Electronics extends Item {
    private String brand;
    private String model;
    private Integer warrantyMonths;
}