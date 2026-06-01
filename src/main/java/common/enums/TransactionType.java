package common.enums;

import java.util.Arrays;

public enum TransactionType {
  DEPOSIT("Nạp"),
  WITHDRAWAL("Rút"),
  AUCTION_PAYMENT("Thanh toán đấu giá"),
  SALE_INCOME("Thu nhập");

  private final String displayName;

  TransactionType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static TransactionType fromValue(String value) {
    if ("Thu nhập bán hàng".equals(value)) {
      return SALE_INCOME;
    }
    return Arrays.stream(values())
        .filter(type -> type.name().equalsIgnoreCase(value) || type.displayName.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Loại giao dịch không hợp lệ"));
  }
}
