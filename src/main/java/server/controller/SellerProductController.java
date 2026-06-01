package server.controller;

import common.dto.SellerProductDTO;
import common.enums.AuctionState;
import common.request.SellerProductRequest;
import common.response.ApiResponse;
import server.service.SellerProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
public class SellerProductController {

  private final SellerProductService sellerProductService;

  @GetMapping
  public ResponseEntity<?> getSellerProducts(
      @RequestParam Long sellerId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) AuctionState status) {
    List<SellerProductDTO> products =
        sellerProductService.getSellerProducts(sellerId, keyword, status);
    return ResponseEntity.ok(products);
  }

  @PostMapping
  public ResponseEntity<ApiResponse<SellerProductDTO>> createSellerProduct(
      @RequestBody SellerProductRequest request) {
    SellerProductDTO product = sellerProductService.saveProductAndPrepareAuction(request);
    return ResponseEntity.ok(
        new ApiResponse<>(true, "Đã tạo sản phẩm và lên lịch phiên đấu giá thành công.", product));
  }

  @PutMapping("/{itemId}")
  public ResponseEntity<ApiResponse<SellerProductDTO>> updateSellerProduct(
      @PathVariable Long itemId, @RequestBody SellerProductRequest request) {
    SellerProductDTO product = sellerProductService.updateOpenProduct(itemId, request);
    return ResponseEntity.ok(new ApiResponse<>(true, "Đã cập nhật sản phẩm thành công.", product));
  }

  @PostMapping("/{itemId}/start")
  public ResponseEntity<ApiResponse<SellerProductDTO>> startSellerProductAuction(
      @PathVariable Long itemId, @RequestParam Long sellerId) {
    SellerProductDTO product = sellerProductService.startOpenAuction(itemId, sellerId);
    return ResponseEntity.ok(new ApiResponse<>(true, "Đã bắt đầu phiên đấu giá.", product));
  }

  @DeleteMapping("/{itemId}")
  public ResponseEntity<ApiResponse<Void>> deleteSellerProduct(
      @PathVariable Long itemId, @RequestParam Long sellerId) {
    sellerProductService.hideProduct(itemId, sellerId);
    return ResponseEntity.ok(
        new ApiResponse<>(true, "Đã xóa sản phẩm khỏi danh sách quản lý.", null));
  }
}
