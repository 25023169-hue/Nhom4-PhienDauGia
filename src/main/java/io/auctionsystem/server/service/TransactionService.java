package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.ChartPointDTO;
import io.auctionsystem.common.dto.RevenueStatsDTO;
import io.auctionsystem.common.dto.TransactionDTO;
import io.auctionsystem.server.model.Transaction;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.TransactionRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Transaction processTransaction(Long userId, Double amount, String type, String note) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Số tiền giao dịch phải lớn hơn 0");
        }

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (!user.isActive()) {
            throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa");
        }

        if ("Nạp".equals(type)) {
            user.setBalance(user.getBalance() + amount);
        } else if ("Rút".equals(type)) {
            if (user.getAvailableBalance() < amount) {
                throw new IllegalArgumentException("Số dư khả dụng không đủ để rút tiền");
            }
            user.setBalance(user.getBalance() - amount);
        } else {
            throw new IllegalArgumentException("Loại giao dịch không hợp lệ");
        }
        userRepository.save(user);

        return saveTransaction(
                userId,
                "Nạp".equals(type) ? amount : 0.0,
                "Rút".equals(type) ? amount : 0.0,
                user.getAvailableBalance(),
                type,
                note
        );
    }

    public Transaction saveTransaction(
            Long userId,
            Double moneyIn,
            Double moneyOut,
            Double lastBalance,
            String type,
            String note
    ) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setNote(note);
        tx.setTransactionTime(LocalDateTime.now());
        tx.setMoneyIn(moneyIn);
        tx.setMoneyOut(moneyOut);
        tx.setLastBalance(lastBalance);

        return transactionRepository.save(tx);
    }

    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionTimeDesc(userId);
    }

    public RevenueStatsDTO getSellerRevenueStats(Long sellerId) {
        List<Transaction> sales = transactionRepository
                .findByUserIdAndTypeInOrderByTransactionTimeDesc(
                        sellerId,
                        List.of("Thu nhập", "Thu nhập bán hàng")
                );

        double totalRevenue = sales.stream()
                .mapToDouble(tx -> tx.getMoneyIn() == null ? 0.0 : tx.getMoneyIn())
                .sum();

        YearMonth currentMonth = YearMonth.now();
        double monthRevenue = sales.stream()
                .filter(tx -> tx.getTransactionTime() != null)
                .filter(tx -> YearMonth.from(tx.getTransactionTime()).equals(currentMonth))
                .mapToDouble(tx -> tx.getMoneyIn() == null ? 0.0 : tx.getMoneyIn())
                .sum();

        long soldOrders = sales.size();
        double averageOrderValue = soldOrders == 0 ? 0.0 : totalRevenue / soldOrders;

        Map<YearMonth, Double> revenueByMonth = new TreeMap<>();
        for (int i = 5; i >= 0; i--) {
            revenueByMonth.put(currentMonth.minusMonths(i), 0.0);
        }
        for (Transaction tx : sales) {
            if (tx.getTransactionTime() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(tx.getTransactionTime());
            if (revenueByMonth.containsKey(month)) {
                revenueByMonth.put(month, revenueByMonth.get(month) + (tx.getMoneyIn() == null ? 0.0 : tx.getMoneyIn()));
            }
        }

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");
        List<ChartPointDTO> monthlyRevenue = new ArrayList<>();
        revenueByMonth.forEach((month, revenue) ->
                monthlyRevenue.add(new ChartPointDTO(month.format(monthFormatter), revenue)));

        List<TransactionDTO> recentSales = sales.stream()
                .limit(20)
                .map(this::toDTO)
                .toList();

        return new RevenueStatsDTO(
                totalRevenue,
                monthRevenue,
                soldOrders,
                averageOrderValue,
                monthlyRevenue,
                recentSales
        );
    }

    private TransactionDTO toDTO(Transaction tx) {
        return new TransactionDTO(
                tx.getId(),
                tx.getUserId(),
                tx.getMoneyIn(),
                tx.getMoneyOut(),
                tx.getLastBalance(),
                tx.getType(),
                tx.getNote(),
                tx.getTransactionTime(),
                tx.getCreatedAt()
        );
    }
}
