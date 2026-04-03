package com.sipapp.repository;

import com.sipapp.entity.Transaction;
import com.sipapp.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySipIdOrderByInstallmentNumberAsc(Long sipId);

    Optional<Transaction> findBySipIdAndStatus(Long sipId, TransactionStatus status);

    Optional<Transaction> findTopBySipIdOrderByInstallmentNumberDesc(Long sipId);

    long countBySipIdAndStatus(Long sipId, TransactionStatus status);
}
