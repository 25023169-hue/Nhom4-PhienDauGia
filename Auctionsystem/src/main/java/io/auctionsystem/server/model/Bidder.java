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
@Table(name = "bidders")
public class Bidder extends User {
    private Double balance;
    private String shippingAddress;
}