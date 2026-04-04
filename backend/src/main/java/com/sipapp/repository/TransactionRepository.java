package com.sipapp.repository;

import com.sipapp.entity.Transaction;
import com.sipapp.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySipIdOrderByInstallmentNumberAsc(Long sipId);

    Optional<Transaction> findBySipIdAndStatus(Long sipId, TransactionStatus status);

    Optional<Transaction> findTopBySipIdOrderByInstallmentNumberDesc(Long sipId);

    long countBySipIdAndStatus(Long sipId, TransactionStatus status);

    /**
     * IDEMPOTENCY CHECK.
     * Before creating a new transaction, verify no transaction
     * already exists for this SIP on this exact date.
     * Prevents duplicate execution if the scheduler fires twice.
     */
    boolean existsBySipIdAndTransactionDate(Long sipId, LocalDate transactionDate);

    /**
     * Check if a specific installment number already exists for a SIP.
     * Second layer of idempotency protection.
     */
    boolean existsBySipIdAndInstallmentNumber(Long sipId, Integer installmentNumber);
}