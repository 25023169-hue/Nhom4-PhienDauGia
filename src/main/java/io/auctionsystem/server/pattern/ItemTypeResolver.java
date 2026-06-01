package io.auctionsystem.server.pattern;

import io.auctionsystem.common.enums.ItemType;
import io.auctionsystem.server.model.Art;
import io.auctionsystem.server.model.Electronics;
import io.auctionsystem.server.model.Fashion;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Jewelry;
import io.auctionsystem.server.model.Vehicle;

public final class ItemTypeResolver {

  private ItemTypeResolver() {}

  public static ItemType resolve(Item item) {
    if (item instanceof Art) return ItemType.ART;
    if (item instanceof Electronics) return ItemType.ELECTRONICS;
    if (item instanceof Vehicle) return ItemType.VEHICLE;
    if (item instanceof Fashion) return ItemType.FASHION;
    if (item instanceof Jewelry) return ItemType.JEWELRY;
    throw new IllegalArgumentException("Loại sản phẩm không được hỗ trợ");
  }
}
