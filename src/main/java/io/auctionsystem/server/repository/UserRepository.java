package io.auctionsystem.server.repository;

import io.auctionsystem.server.model.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :userId")
  Optional<User> findByIdForUpdate(@Param("userId") Long userId);

  // Dành cho Admin: Chỉ lấy những User thuộc nhóm Bidder hoặc Seller
  @Query("SELECT u FROM User u WHERE TYPE(u) IN (Bidder, Seller)")
  List<User> findAllClients();

  // Xóa quyền Bidder
  @Modifying(clearAutomatically = true) // Ép xóa cache sau khi chạy query
  @Transactional
  @Query(value = "DELETE FROM bidders WHERE id = :userId", nativeQuery = true)
  void removeBidderRole(@Param("userId") Long userId);

  // Nâng cấp lên Seller
  @Modifying(clearAutomatically = true) // Ép xóa cache sau khi chạy query
  @Transactional
  @Query(
      value = "INSERT INTO sellers (id, store_name) VALUES (:userId, :storeName)",
      nativeQuery = true)
  void upgradeToSellerNative(@Param("userId") Long userId, @Param("storeName") String storeName);

  // Kiểm tra xem User có phải là Seller không
  @Query(value = "SELECT COUNT(*) FROM sellers WHERE id = :userId", nativeQuery = true)
  int isUserSeller(@Param("userId") Long userId);

  @Query(value = "SELECT id FROM sellers", nativeQuery = true)
  List<Long> findSellerIds();
}
