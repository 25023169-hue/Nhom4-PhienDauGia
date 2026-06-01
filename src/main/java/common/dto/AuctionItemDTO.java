package common.dto;

import common.enums.AuctionState;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuctionItemDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  // LỖI ĐÃ SỬA: Đổi "Id" (viết hoa) thành "id" (viết thường)
  // Khi field tên là "Id", Lombok tạo getter tên là "getId()" NHƯNG
  // PropertyValueFactory<>("id") ở Controller tìm getter "getId()" theo
  // convention JavaFX Bean → có thể gây cột bảng trống tùy version Lombok.
  // Đặt tên "id" chuẩn Java Bean convention để Lombok tạo đúng "getId()".
  private Long id;
  private Long itemId;

  private String name;
  private Double currentPrice;
  private String startTime;
  private String endTime;
  private AuctionState status;
}
