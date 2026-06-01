package io.auctionsystem.server.pattern;

import io.auctionsystem.common.request.ItemRequest;
import io.auctionsystem.server.exception.ValidationException;
import io.auctionsystem.server.model.Art;
import io.auctionsystem.server.model.Electronics;
import io.auctionsystem.server.model.Fashion;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Jewelry;
import io.auctionsystem.server.model.Vehicle;

public class ItemFactory {

  // Pattern: Factory Method để tạo đối tượng đa hình
  public static Item createItem(ItemRequest request) {
    if (request.getType() == null) {
      throw new ValidationException("Loại sản phẩm không được để trống");
    }

    Item item;
    switch (request.getType()) {
      case ELECTRONICS:
        item = new Electronics();
        break;
      case ART:
        item = new Art();
        break;
      case VEHICLE:
        item = new Vehicle();
        break;
      case FASHION:
        Fashion fashion = new Fashion();
        fashion.setBrand(request.getBrand());
        fashion.setSize(request.getSize());
        fashion.setMaterial(request.getMaterial());
        fashion.setGender(request.getGender());
        item = fashion;
        break;
      case JEWELRY:
        Jewelry jewelry = new Jewelry();
        jewelry.setMaterial(request.getMaterial());
        jewelry.setWeight(request.getWeight());
        jewelry.setGemstone(request.getGemstone());
        item = jewelry;
        break;
      default:
        throw new ValidationException("Loại sản phẩm không hỗ trợ!");
    }

    // Đổ các thông tin chung của Item
    item.setName(request.getName());
    item.setDescription(request.getDescription());
    item.setStartingPrice(request.getStartingPrice());

    return item;
  }
}
