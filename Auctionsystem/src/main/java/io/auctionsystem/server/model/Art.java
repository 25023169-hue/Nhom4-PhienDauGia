package io.auctionsystem.server.model;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
 @Table(name = "art_items")
//@DiscriminatorValue("ART")
public class Art extends Item {

    private String artistName;     // Tên họa sĩ/Tác giả
    private String medium;         // Chất liệu (Sơn dầu, màu nước, điêu khắc...)
    private String dimensions;     // Kích thước (ví dụ: 60x80cm)
    private Integer creationYear;  // Năm sáng tác

}