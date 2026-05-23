package io.auctionsystem.server.repository;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.SellerProductListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerProductListingRepository extends JpaRepository<SellerProductListing, Long> {
    List<SellerProductListing> findBySellerIdOrderByIdDesc(Long sellerId);
    List<SellerProductListing> findBySellerIdAndStatusOrderByIdDesc(Long sellerId, AuctionState status);
}
