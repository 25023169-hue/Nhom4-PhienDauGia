package server.repository;

import server.model.Item;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

  // Tìm tất cả sản phẩm của một người bán cụ thể
  // Giả sử trong model Item bạn có trường sellerId (hoặc quan hệ ManyToOne với Seller)
  List<Item> findBySellerId(Long sellerId);

  // Tìm kiếm sản phẩm theo tên (hỗ trợ tìm kiếm gần đúng - LIKE %name%)
  List<Item> findByNameContainingIgnoreCase(String name);
}
