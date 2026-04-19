package io.auctionsystem.model;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sellers")
public class Seller extends User {
    private String storeName;
    private Double rating;
}