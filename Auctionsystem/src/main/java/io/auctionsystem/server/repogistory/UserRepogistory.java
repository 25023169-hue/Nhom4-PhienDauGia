package io.auctionsystem.server.repogistory;

import io.auctionsystem.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepogistory extends JpaRepository<User, Long> {

    // Tự động generate SQL: SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // Kiểm tra xem username đã tồn tại chưa (phục vụ chức năng Đăng ký)
    boolean existsByUsername(String username);
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "INSERT INTO sellers (id, store_name, rating) VALUES (:userId, :storeName, 0.0)", nativeQuery = true)
    void upgradeToSeller(@Param("userId") Long userId, @Param("storeName") String storeName);
}