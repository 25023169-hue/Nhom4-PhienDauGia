package io.auctionsystem.server.repository;

import io.auctionsystem.server.model.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  List<Transaction> findByUserIdOrderByTransactionTimeDesc(Long userId);

  List<Transaction> findByUserIdAndTypeOrderByTransactionTimeDesc(Long userId, String type);

  List<Transaction> findByUserIdAndTypeInOrderByTransactionTimeDesc(
      Long userId, List<String> types);
}
