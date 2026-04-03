package com.sipapp.scheduler;

import com.sipapp.entity.Sip;
import com.sipapp.entity.Transaction;
import com.sipapp.enums.SipStatus;
import com.sipapp.enums.TransactionStatus;
import com.sipapp.repository.SipRepository;
import com.sipapp.repository.TransactionRepository;
import com.sipapp.service.SipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class SipScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SipScheduler.class);

    @Autowired private SipRepository sipRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private SipService sipService;

    /**
     * Runs every day at midnight.
     * For each ACTIVE SIP, checks if the pending transaction date has arrived.
     * Marks it COMPLETED, creates next transaction, or marks SIP as COMPLETED.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processDailySips() {
        LocalDate today = LocalDate.now();
        logger.info("[SCHEDULER] Running SIP scheduler for date: {}", today);

        List<Sip> activeSips = sipRepository.findByStatus(SipStatus.ACTIVE);
        logger.info("[SCHEDULER] Found {} active SIPs to process", activeSips.size());

        for (Sip sip : activeSips) {
            try {
                processSingleSip(sip, today);
            } catch (Exception e) {
                // One SIP failure must NOT stop others
                logger.error("[SCHEDULER] Error processing SIP ID {}: {}", sip.getId(), e.getMessage(), e);
            }
        }

        logger.info("[SCHEDULER] Scheduler run complete for {}", today);
    }

    private void processSingleSip(Sip sip, LocalDate today) {
        String passbookId = sip.getPassbook().getId();

        // Find the current PENDING transaction for this SIP
        Optional<Transaction> pendingOpt = transactionRepository
                .findBySipIdAndStatus(sip.getId(), TransactionStatus.PENDING);

        if (pendingOpt.isEmpty()) {
            logger.warn("[SIP] {} → SIP ID {} has no PENDING transaction, skipping",
                    passbookId, sip.getId());
            return;
        }

        Transaction pending = pendingOpt.get();

        // Check if today is on or after the scheduled transaction date
        if (!today.isAfter(pending.getTransactionDate()) && !today.isEqual(pending.getTransactionDate())) {
            // Not due yet
            return;
        }

        // Mark current transaction as COMPLETED
        pending.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(pending);

        logger.info("[SIP] {} → Installment {} completed (SIP ID: {})",
                passbookId, pending.getInstallmentNumber(), sip.getId());

        int nextInstallmentNumber = pending.getInstallmentNumber() + 1;

        // Check if all installments are done
        if (nextInstallmentNumber > sip.getTotalInstallments()) {
            sip.setStatus(SipStatus.COMPLETED);
            sipRepository.save(sip);
            logger.info("[SIP] {} → SIP ID {} fully completed ({} installments)",
                    passbookId, sip.getId(), sip.getTotalInstallments());
            return;
        }

        // Calculate next transaction date based on frequency
        LocalDate nextDate = calculateNextDate(pending.getTransactionDate(), sip);

        // Idempotency: don't create if already exists for this installment
        boolean exists = transactionRepository
                .findBySipIdAndStatus(sip.getId(), TransactionStatus.PENDING)
                .isPresent();

        if (!exists) {
            Transaction nextTransaction = sipService.buildTransaction(sip, nextInstallmentNumber, nextDate);
            transactionRepository.save(nextTransaction);

            logger.info("[SIP] {} → Installment {} scheduled for {} (SIP ID: {})",
                    passbookId, nextInstallmentNumber, nextDate, sip.getId());
        }
    }

    /**
     * Calculates next transaction date based on SIP frequency.
     * Java's LocalDate handles edge cases like Feb 28 automatically.
     */
    private LocalDate calculateNextDate(LocalDate currentDate, Sip sip) {
        return switch (sip.getFrequency()) {
            case DAILY   -> currentDate.plusDays(1);
            case WEEKLY  -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);  // Auto-handles Feb 28/29
            case YEARLY  -> currentDate.plusYears(1);
        };
    }
}
