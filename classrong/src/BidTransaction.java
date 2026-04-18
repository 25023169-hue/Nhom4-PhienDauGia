import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private int auctionId;
    private int bidderId;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction() {
    }

    public BidTransaction(int id, int auctionId, int bidderId, double bidAmount, LocalDateTime bidTime) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getBidderId() {
        return bidderId;
    }

    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
}