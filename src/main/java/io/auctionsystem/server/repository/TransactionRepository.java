package io.auctionsystem.server.repository;

import io.auctionsystem.server.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByTransactionTimeDesc(Long userId);
    List<Transaction> findByUserIdAndTypeOrderByTransactionTimeDesc(Long userId, String type);
}
