package server.service;

import common.enums.AuctionState;
import common.response.BidResponse;
import server.model.Auction;
import server.model.Bid;
import server.model.Item;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AuctionNotificationService implements InitializingBean {

  private final BidRepository bidRepository;
  private final AuctionRepository auctionRepository;
  private final ItemRepository itemRepository;
  private final NotificationService notificationService;
  private final Set<Long> processedAuctions = Collections.synchronizedSet(new HashSet<>());

  public AuctionNotificationService(
      BidRepository bidRepository,
      AuctionRepository auctionRepository,
      ItemRepository itemRepository,
      NotificationService notificationService) {
    this.bidRepository = bidRepository;
    this.auctionRepository = auctionRepository;
    this.itemRepository = itemRepository;
    this.notificationService = notificationService;
  }

  @Override
  public void afterPropertiesSet() {
    auctionRepository.findAll().stream()
        .filter(auction -> isTerminal(auction.getStatus()))
        .map(Auction::getId)
        .forEach(processedAuctions::add);
  }

  public void notifyBidAfterCommit(BidResponse response, Long bidderId) {
    afterCommit(
        () ->
            sendBidNotifications(
                response.getAuctionId(),
                response.getPreviousWinnerId(),
                bidderId,
                response.getNewCurrentPrice()));
  }

  public void notifyExpiredAuctionAfterCommit(Long auctionId) {
    afterCommit(() -> sendExpiredAuctionNotifications(auctionId));
  }

  public void notifyFinishedAuctionAfterCommit(Long auctionId) {
    afterCommit(() -> sendFinishNotifications(auctionId));
  }

  public void notifyCancelledAuctionAfterCommit(Long auctionId) {
    afterCommit(() -> sendCancelNotifications(auctionId));
  }

  private void sendBidNotifications(
      Long auctionId, Long previousBidderId, Long newBidderId, Double newPrice) {
    Item item = getItemByAuctionId(auctionId);
    Long sellerId = item == null || item.getSeller() == null ? null : item.getSeller().getId();
    notificationService.createNotification(
        newBidderId,
        "Bạn đã đặt giá thành công mức " + formatVND(newPrice) + " cho phiên đấu giá #" + auctionId,
        "BID_SUCCESS");
    if (previousBidderId != null && !previousBidderId.equals(newBidderId)) {
      notificationService.createNotification(
          previousBidderId,
          "Bạn đã bị vượt giá tại phiên sản phẩm #" + auctionId + "!",
          "BID_OUTBID");
    }
    if (sellerId != null && !sellerId.equals(newBidderId)) {
      notificationService.createNotification(
          sellerId,
          "Có lượt đặt giá mới: " + formatVND(newPrice) + " cho sản phẩm của bạn.",
          "NEW_BID");
    }
  }

  private void sendExpiredAuctionNotifications(Long auctionId) {
    Auction auction = auctionRepository.findById(auctionId).orElse(null);
    if (auction != null && auction.getStatus() == AuctionState.FINISHED) {
      sendFinishNotifications(auctionId);
      return;
    }
    if (auction == null
        || auction.getStatus() != AuctionState.CANCELLED
        || !processedAuctions.add(auctionId)) {
      return;
    }

    Item item = getItemByAuctionId(auctionId);
    if (item != null && item.getSeller() != null) {
      notificationService.createNotification(
          item.getSeller().getId(),
          "Phiên đấu giá #" + auctionId + " đã kết thúc nhưng không có người mua.",
          "AUCTION_EXPIRED");
    }
  }

  private void sendFinishNotifications(Long auctionId) {
    if (!processedAuctions.add(auctionId)) {
      return;
    }

    Auction auction = auctionRepository.findById(auctionId).orElse(null);
    if (auction == null || auction.getStatus() != AuctionState.FINISHED) {
      processedAuctions.remove(auctionId);
      return;
    }

    Item item = getItemByAuctionId(auctionId);
    Long sellerId = item == null || item.getSeller() == null ? null : item.getSeller().getId();
    Set<Long> bidderIds =
        bidRepository.findByAuctionId(auctionId).stream()
            .map(Bid::getBidderId)
            .collect(Collectors.toSet());
    Long winnerId = auction.getWinnerId();
    if (winnerId != null) {
      notificationService.createNotification(
          winnerId, "Bạn đã thắng phiên đấu giá #" + auctionId + "!", "BID_WON");
      bidderIds.stream()
          .filter(bidderId -> !bidderId.equals(winnerId))
          .forEach(
              bidderId ->
                  notificationService.createNotification(
                      bidderId,
                      "Phiên #" + auctionId + " đã kết thúc. Bạn không trúng giải.",
                      "BID_LOST"));
      if (sellerId != null) {
        notificationService.createNotification(
            sellerId,
            "Sản phẩm của bạn đã bán thành công với giá "
                + formatVND(auction.getFinalPrice())
                + ".",
            "AUCTION_SOLD");
      }
    }
  }

  private void sendCancelNotifications(Long auctionId) {
    if (!processedAuctions.add(auctionId)) {
      return;
    }

    Auction auction = auctionRepository.findById(auctionId).orElse(null);
    if (auction == null || auction.getStatus() != AuctionState.CANCELLED) {
      processedAuctions.remove(auctionId);
      return;
    }

    Item item = getItemByAuctionId(auctionId);
    if (item != null && item.getSeller() != null) {
      notificationService.createNotification(
          item.getSeller().getId(),
          "Phiên đấu giá #" + auctionId + " đã bị hủy.",
          "AUCTION_CANCELED");
    }
    bidRepository.findByAuctionId(auctionId).stream()
        .map(Bid::getBidderId)
        .distinct()
        .forEach(
            bidderId ->
                notificationService.createNotification(
                    bidderId, "Phiên đấu giá #" + auctionId + " đã bị hủy.", "AUCTION_CANCELED"));
  }

  private Item getItemByAuctionId(Long auctionId) {
    return auctionRepository
        .findById(auctionId)
        .map(Auction::getItemId)
        .flatMap(itemRepository::findById)
        .orElse(null);
  }

  private boolean isTerminal(AuctionState status) {
    return status == AuctionState.FINISHED || status == AuctionState.CANCELLED;
  }

  private void afterCommit(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            action.run();
          }
        });
  }

  private String formatVND(Double amount) {
    return NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"))
            .format(amount == null ? 0 : amount)
        + "đ";
  }
}
