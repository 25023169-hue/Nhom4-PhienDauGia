package server.repository;

import common.enums.AuctionState;
import server.model.SellerProductListing;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerProductListingRepository extends JpaRepository<SellerProductListing, Long> {
  List<SellerProductListing> findBySellerIdOrderByIdDesc(Long sellerId);

  List<SellerProductListing> findBySellerIdAndStatusOrderByIdDesc(
      Long sellerId, AuctionState status);

  Optional<SellerProductListing> findByItemId(Long itemId);
}
