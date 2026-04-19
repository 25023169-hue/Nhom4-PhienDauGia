public class Vehicle extends Item {

    public Vehicle() {
    }

    public Vehicle(int id, String name, String description, double startingPrice, double currentPrice, int sellerId) {
        super(id, name, description, startingPrice, currentPrice, sellerId);
    }
}