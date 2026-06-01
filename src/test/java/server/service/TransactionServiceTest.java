package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import common.enums.TransactionType;
import server.exception.ResourceNotFoundException;
import server.exception.ValidationException;
import server.exception.WalletException;
import server.model.Bidder;
import server.model.Transaction;
import server.repository.TransactionRepository;
import server.repository.UserRepository;
import server.model.converter.TransactionTypeConverter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TransactionServiceTest {

  @InjectMocks private TransactionService transactionService;
  @Mock private TransactionRepository transactionRepository;
  @Mock private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void processTransaction_DepositAndWithdraw_UpdateBalance() {
    Bidder user = bidder(1000.0, 100.0);
    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

    Transaction deposit = transactionService.processTransaction(1L, 500.0, "Nạp", "deposit");
    Transaction withdraw = transactionService.processTransaction(1L, 300.0, "Rút", "withdraw");

    assertEquals(1200.0, user.getBalance());
    assertEquals(500.0, deposit.getMoneyIn());
    assertEquals(0.0, deposit.getMoneyOut());
    assertEquals(0.0, withdraw.getMoneyIn());
    assertEquals(300.0, withdraw.getMoneyOut());
    verify(userRepository, org.mockito.Mockito.times(2)).save(user);
  }

  @Test
  void processTransaction_RejectsInvalidInput() {
    assertThrows(
        ValidationException.class,
        () -> transactionService.processTransaction(1L, 0.0, "Nạp", null));
    assertThrows(
        ValidationException.class,
        () -> transactionService.processTransaction(1L, null, "Nạp", null));

    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> transactionService.processTransaction(1L, 100.0, "Nạp", null));

    Bidder user = bidder(100.0, 0.0);
    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
    assertThrows(
        WalletException.class,
        () -> transactionService.processTransaction(1L, 200.0, "Rút", null));
    assertThrows(
        ValidationException.class,
        () -> transactionService.processTransaction(1L, 100.0, "Khác", null));
  }

  @Test
  void getTransactions_ReturnsRepositoryResult() {
    List<Transaction> transactions = List.of(new Transaction());
    when(transactionRepository.findByUserIdOrderByTransactionTimeDesc(1L))
        .thenReturn(transactions);

    assertEquals(transactions, transactionService.getTransactionsByUserId(1L));
  }

  @Test
  void getSellerRevenueStats_AggregatesSalesAndBuildsChart() {
    Transaction currentMonth = transaction(1L, 200.0, LocalDateTime.now());
    Transaction previousMonth = transaction(2L, 100.0, LocalDateTime.now().minusMonths(1));
    Transaction withoutDate = transaction(3L, null, null);
    when(transactionRepository.findByUserIdOrderByTransactionTimeDesc(10L))
        .thenReturn(List.of(currentMonth, previousMonth, withoutDate));

    var stats = transactionService.getSellerRevenueStats(10L);

    assertEquals(300.0, stats.getTotalRevenue());
    assertEquals(200.0, stats.getMonthRevenue());
    assertEquals(3, stats.getSoldOrders());
    assertEquals(100.0, stats.getAverageOrderValue());
    assertEquals(6, stats.getMonthlyRevenue().size());
    assertEquals(3, stats.getRecentSales().size());
  }

  @Test
  void getSellerRevenueStats_NoSales_ReturnsZeroAverage() {
    when(transactionRepository.findByUserIdOrderByTransactionTimeDesc(10L)).thenReturn(List.of());

    assertEquals(0.0, transactionService.getSellerRevenueStats(10L).getAverageOrderValue());
  }

  @Test
  void transactionTypeConverter_ReadsLegacyValuesAndWritesStableEnumNames() {
    TransactionTypeConverter converter = new TransactionTypeConverter();

    assertEquals(TransactionType.DEPOSIT, converter.convertToEntityAttribute("Nạp"));
    assertEquals(TransactionType.SALE_INCOME, converter.convertToEntityAttribute("Thu nhập bán hàng"));
    assertEquals("WITHDRAWAL", converter.convertToDatabaseColumn(TransactionType.WITHDRAWAL));
  }

  private Bidder bidder(double balance, double heldBalance) {
    Bidder bidder = new Bidder();
    bidder.setBalance(balance);
    bidder.setHeldBalance(heldBalance);
    return bidder;
  }

  private Transaction transaction(Long id, Double moneyIn, LocalDateTime time) {
    Transaction transaction = new Transaction();
    transaction.setId(id);
    transaction.setUserId(10L);
    transaction.setMoneyIn(moneyIn);
    transaction.setMoneyOut(0.0);
    transaction.setLastBalance(100.0);
    transaction.setType(TransactionType.SALE_INCOME);
    transaction.setNote("sale");
    transaction.setTransactionTime(time);
    return transaction;
  }
}
