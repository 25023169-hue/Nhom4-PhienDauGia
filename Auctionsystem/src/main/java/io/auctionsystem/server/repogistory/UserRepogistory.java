package io.auctionsystem.server.repogistory;

import io.auctionsystem.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepogistory extends JpaRepository<User, Long> {

    // Tự động generate SQL: SELECT * FROM users WHERE username = ?
    Optional<User> findByUsername(String username);

    // Kiểm tra xem username đã tồn tại chưa (phục vụ chức năng Đăng ký)
    boolean existsByUsername(String username);
}