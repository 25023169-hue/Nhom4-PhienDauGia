package io.auctionsystem.server.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // BẮT BUỘC: Để MySQL tự tăng ID
    private Long Id; // CHUYỂN SANG LONG: Chuẩn chung cho Database lớn
}