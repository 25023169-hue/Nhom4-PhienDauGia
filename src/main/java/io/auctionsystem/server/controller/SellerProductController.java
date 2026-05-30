package io.auctionsystem.server.controller;

import io.auctionsystem.common.dto.SellerProductDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.request.SellerProductRequest;
import io.auctionsystem.common.response.ApiResponse;
import io.auctionsystem.server.service.SellerProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    @Autowired
    private SellerProductService sellerProductService;

    @GetMapping
    public ResponseEntity<?> getSellerProducts(
            @RequestParam Long sellerId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AuctionState status
    ) {
        try {
            List<SellerProductDTO> products = sellerProductService.getSellerProducts(sellerId, keyword, status);
            return ResponseEntity.ok(products);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Lỗi Server: " + e.getMessage(), null));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SellerProductDTO>> createSellerProduct(@RequestBody SellerProductRequest request) {
        try {
            SellerProductDTO product = sellerProductService.saveProductAndPrepareAuction(request);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Đã tạo sản phẩm và lên lịch phiên đấu giá thành công.",
                    product
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Lỗi Server: " + e.getMessage(), null));
        }
    }
}
