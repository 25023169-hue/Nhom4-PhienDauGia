package io.auctionsystem.server.pattern;

import io.auctionsystem.common.dto.ItemRequest;
import io.auctionsystem.common.enums.ItemType;
import io.auctionsystem.server.model.Art;
import io.auctionsystem.server.model.Electronics;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class ItemFactory {

    // Pattern: Factory Method để tạo đối tượng đa hình
    public Item createItem(Item item) {
        if(item instanceof Art art) {
            return art;
        } else if (item instanceof Electronics electronics) {
            return electronics;
        } else if (item instanceof Vehicle vehicle) {
            return vehicle;
        }

        throw new IllegalArgumentException("Loại sản phẩm không được để trống");
    }
}