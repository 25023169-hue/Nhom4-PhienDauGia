package server.pattern;

import common.enums.ItemType;
import server.exception.ValidationException;
import server.model.Art;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Item;
import server.model.Jewelry;
import server.model.Vehicle;

public final class ItemTypeResolver {

  private ItemTypeResolver() {}

  public static ItemType resolve(Item item) {
    if (item instanceof Art) return ItemType.ART;
    if (item instanceof Electronics) return ItemType.ELECTRONICS;
    if (item instanceof Vehicle) return ItemType.VEHICLE;
    if (item instanceof Fashion) return ItemType.FASHION;
    if (item instanceof Jewelry) return ItemType.JEWELRY;
    throw new ValidationException("Loại sản phẩm không được hỗ trợ");
  }
}
