package io.auctionsystem.server.pattern;

import io.auctionsystem.common.request.ItemRequest;
import io.auctionsystem.server.model.Art;
import io.auctionsystem.server.model.Electronics;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Vehicle;

public class ItemFactory {

    // Pattern: Factory Method để tạo đối tượng đa hình
    public static Item createItem(ItemRequest request) {
        if (request.getType() == null) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống");
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
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hỗ trợ!");
        }

        // Chỉ đổ các thông tin chung của Item
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setStartingPrice(request.getStartingPrice());

        return item;
    }
}