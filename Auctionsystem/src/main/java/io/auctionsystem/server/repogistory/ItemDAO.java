package io.auctionsystem.server.repogistory;

import io.auctionsystem.server.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemDAO extends JpaRepository<Item, Long> {

    // Tìm tất cả sản phẩm của một người bán cụ thể
    // Giả sử trong model Item bạn có trường sellerId (hoặc quan hệ ManyToOne với Seller)
    List<Item> findBySellerId(Long sellerId);

    // Tìm kiếm sản phẩm theo tên (hỗ trợ tìm kiếm gần đúng - LIKE %name%)
    List<Item> findByNameContainingIgnoreCase(String name);
}