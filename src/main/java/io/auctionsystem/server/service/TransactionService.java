package io.auctionsystem.server.service;

import io.auctionsystem.server.model.Transaction;
import io.auctionsystem.server.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getTransactionHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionTimeDesc(userId);
    }

    @Transactional
    public Transaction processTransaction(Long userId, Double amount, String type, String note, Double currentBalance) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setType(type);

        double newBalance = currentBalance;

        if ("NẠP".equalsIgnoreCase(type)) {
            transaction.setMoneyIn(amount);
            transaction.setMoneyOut(0.0);
            newBalance = currentBalance + amount;
        } else if ("RÚT".equalsIgnoreCase(type)) {
            transaction.setMoneyIn(0.0);
            transaction.setMoneyOut(amount);
            newBalance = currentBalance - amount;
        }

        transaction.setLastBalance(newBalance);
        transaction.setNote(note != null ? note : type + " tiền");


        transaction.setTransactionTime(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }
}