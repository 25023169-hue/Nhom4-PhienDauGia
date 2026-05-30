package io.auctionsystem.server.service;

import io.auctionsystem.server.model.Transaction;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.TransactionRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
}
