package io.auctionsystem.server.service;

import io.auctionsystem.server.model.Transaction;
import io.auctionsystem.server.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction processTransaction(Long userId, Double amount, String type, String note, Double currentBalance) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setNote(note);
        tx.setTransactionTime(LocalDateTime.now()); // Dùng đúng biến transactionTime

        // Tự động phân bổ vào tiền vào/tiền ra và tính số dư
        if ("Nạp".equals(type)) {
            tx.setMoneyIn(amount);
            tx.setMoneyOut(0.0);
            tx.setLastBalance(currentBalance + amount);
        } else if ("Rút".equals(type)) {
            tx.setMoneyIn(0.0);
            tx.setMoneyOut(amount);
            tx.setLastBalance(currentBalance - amount);
        }

        return transactionRepository.save(tx);
    }

    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserId(userId);
    }
}