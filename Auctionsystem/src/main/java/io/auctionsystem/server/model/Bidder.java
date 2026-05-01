package io.auctionsystem.server.model;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "bidders")
public class Bidder extends User {
    private String shippingAddress;
}