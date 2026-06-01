package server.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import common.enums.ItemType;
import common.request.ItemRequest;
import server.exception.ValidationException;
import server.model.Art;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Jewelry;
import server.model.Vehicle;
import org.junit.jupiter.api.Test;

class ItemFactoryTest {

  @Test
  void createItem_CreatesSupportedTypes() {
    assertInstanceOf(Electronics.class, ItemFactory.createItem(request(ItemType.ELECTRONICS)));
    assertInstanceOf(Art.class, ItemFactory.createItem(request(ItemType.ART)));
    assertInstanceOf(Vehicle.class, ItemFactory.createItem(request(ItemType.VEHICLE)));

    Fashion fashion = (Fashion) ItemFactory.createItem(request(ItemType.FASHION));
    assertEquals("brand", fashion.getBrand());
    assertEquals("size", fashion.getSize());
    assertEquals("material", fashion.getMaterial());
    assertEquals("gender", fashion.getGender());

    Jewelry jewelry = (Jewelry) ItemFactory.createItem(request(ItemType.JEWELRY));
    assertEquals("material", jewelry.getMaterial());
    assertEquals(1.0, jewelry.getWeight());
    assertEquals("gemstone", jewelry.getGemstone());
  }

  @Test
  void createItem_MissingType_ThrowsValidationException() {
    assertThrows(ValidationException.class, () -> ItemFactory.createItem(request(null)));
  }

  private ItemRequest request(ItemType type) {
    ItemRequest request = new ItemRequest();
    request.setType(type);
    request.setName("name");
    request.setDescription("description");
    request.setStartingPrice(100.0);
    request.setBrand("brand");
    request.setSize("size");
    request.setMaterial("material");
    request.setGender("gender");
    request.setWeight(1.0);
    request.setGemstone("gemstone");
    return request;
  }
}
