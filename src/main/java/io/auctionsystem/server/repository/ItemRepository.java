package io.auctionsystem.server.repository;

import io.auctionsystem.server.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // SỬA: Dùng @Query vì field trong Item là "seller" (kiểu User/ManyToOne),
    // không phải "sellerId" (Long) → Spring Data không tự generate được.
    @Query("SELECT i FROM Item i WHERE i.seller.id = :sellerId")
    List<Item> findBySellerId(@Param("sellerId") Long sellerId);

    // Giữ nguyên, không đổi
    List<Item> findByNameContainingIgnoreCase(String name);
}