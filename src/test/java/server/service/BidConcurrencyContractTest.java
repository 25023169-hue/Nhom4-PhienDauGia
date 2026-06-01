package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import common.request.BidRequest;
import server.repository.AuctionRepository;
import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

class BidConcurrencyContractTest {

  @Test
  void auctionLookupUsesDatabaseWriteLock() throws NoSuchMethodException {
    Method method = AuctionRepository.class.getMethod("findByIdForUpdate", Long.class);

    Lock lock = method.getAnnotation(Lock.class);

    assertNotNull(lock);
    assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
  }

  @Test
  void placeBidDoesNotSerializeUnrelatedAuctionsAtServiceLevel() throws NoSuchMethodException {
    Method method = BidService.class.getMethod("placeBid", BidRequest.class);

    assertFalse(Modifier.isSynchronized(method.getModifiers()));
  }
}
