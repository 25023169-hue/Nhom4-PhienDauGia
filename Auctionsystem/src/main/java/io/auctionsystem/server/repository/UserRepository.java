package io.auctionsystem.server.repository;

import io.auctionsystem.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    // THÊM MỚI: Hàm xóa quyền Bidder
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM bidders WHERE id = :userId", nativeQuery = true)
    void removeBidderRole(@Param("userId") Long Id);

    // Hàm nâng cấp Seller (Sửa lại tên tham số cho đồng bộ)
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO sellers (id, store_name) VALUES (:userId, :storeName)", nativeQuery = true)
    void upgradeToSellerNative(@Param("userId") Long Id, @Param("storeName") String storeName);

    // Hàm kiểm tra
    @Query(value = "SELECT COUNT(*) FROM sellers WHERE id = :Id", nativeQuery = true)
    int isUserSeller(@Param("Id") Long Id);
}