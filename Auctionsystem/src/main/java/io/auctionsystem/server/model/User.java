package io.auctionsystem.server.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User extends BaseEntity {

    // Thêm ràng buộc duy nhất (unique = true) và không được để trống (nullable = false)
    @Column(unique = true, nullable = false)
    private String username;

    // Mật khẩu cũng không nên để trống
    @Column(nullable = false)
    private String password;

    private String fullName;
    private String email;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Item> items = new ArrayList<>();
}