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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class SipScheduler {

    private static final Logger log = LoggerFactory.getLogger(SipScheduler.class);

    @Autowired private SipRepository sipRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private SipService sipService;

    @Value("${app.scheduler.mode:PROD}")
    private String schedulerMode;

    // ══════════════════════════════════════════════════════════════════
    // SCHEDULER ENTRY POINT
    //
    // Cron is driven by the active Spring profile:
    //   TEST profile  → "0 * * * * *"   (every 1 minute)
    //   PROD profile  → "0 0 0 * * *"   (every day at midnight)
    //
    // The scheduler ONLY fetches SIPs that are:
    //   1. status = ACTIVE         → PAUSED and COMPLETED are automatically skipped
    //   2. nextExecutionDate <= today  → not-yet-due SIPs are skipped
    // ══════════════════════════════════════════════════════════════════

    @Scheduled(cron = "${app.scheduler.cron:0 0 0 * * *}")
    public void processDueSips() {
        LocalDate today = LocalDate.now();
        log.info("[SCHEDULER][{}] ── Tick at {} ──────────────────────────", schedulerMode, today);

        // The query does the heavy lifting: only ACTIVE + due SIPs come back.
        // PAUSED and COMPLETED SIPs never reach this code.
        List<Sip> dueSips = sipRepository.findDueSips(SipStatus.ACTIVE, today);

        if (dueSips.isEmpty()) {
            log.info("[SCHEDULER] No SIPs due today.");
            return;
        }

        log.info("[SCHEDULER] {} SIP(s) due for execution", dueSips.size());

        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        for (Sip sip : dueSips) {
            try {
                boolean executed = processSingleSip(sip, today);
                if (executed) successCount++;
                else skipCount++;
            } catch (Exception e) {
                errorCount++;
                // CRITICAL: one failed SIP must NEVER stop others
                log.error("[SCHEDULER][ERROR] SIP id={} passbook={} — {}",
                        sip.getId(), sip.getPassbook().getId(), e.getMessage(), e);
            }
        }

        log.info("[SCHEDULER] Done — executed={} skipped(idempotent)={} errors={}",
                successCount, skipCount, errorCount);
    }

    // ══════════════════════════════════════════════════════════════════
    // PROCESS A SINGLE SIP
    //
    // Returns true  → transaction was executed
    //         false → skipped (idempotency guard triggered)
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public boolean processSingleSip(Sip sip, LocalDate today) {
        String passbookId = sip.getPassbook().getId();
        int nextInstallmentNumber = sip.getCompletedInstallments() + 1;

        // ── IDEMPOTENCY CHECK 1: by date ──────────────────────────────
        // If a transaction already exists for this SIP on today's date,
        // the scheduler must have already run. Skip silently.
        if (transactionRepository.existsBySipIdAndTransactionDate(sip.getId(), today)) {
            log.warn("[SCHEDULER][SKIP][idempotent-date] SIP={} passbook={} date={}",
                    sip.getId(), passbookId, today);
            return false;
        }

        // ── IDEMPOTENCY CHECK 2: by installment number ────────────────
        // Prevents duplicate if the scheduler somehow fires twice in a day.
        if (transactionRepository.existsBySipIdAndInstallmentNumber(sip.getId(), nextInstallmentNumber)) {
            log.warn("[SCHEDULER][SKIP][idempotent-installment] SIP={} installment={}",
                    sip.getId(), nextInstallmentNumber);
            return false;
        }

        // ── BUILD AND SAVE TRANSACTION ────────────────────────────────
        Transaction tx = sipService.buildTransaction(sip, nextInstallmentNumber, today);
        tx.setStatus(TransactionStatus.COMPLETED);  // executed immediately by scheduler
        transactionRepository.save(tx);

        log.info("[SIP][EXECUTED] passbook={} sipId={} installment={}/{} amount={} interest={}",
                passbookId, sip.getId(),
                nextInstallmentNumber, sip.getTotalInstallments(),
                tx.getAmount(), tx.getInterest());

        // ── UPDATE SIP COUNTERS ───────────────────────────────────────
        int newCompleted = sip.getCompletedInstallments() + 1;
        int newRemaining = sip.getTotalInstallments() - newCompleted;

        sip.setCompletedInstallments(newCompleted);
        sip.setRemainingInstallments(newRemaining);

        // ── CHECK COMPLETION ──────────────────────────────────────────
        if (newCompleted >= sip.getTotalInstallments()) {
            sip.setStatus(SipStatus.COMPLETED);
            sip.setNextExecutionDate(null);   // no more executions
            log.info("[SIP][COMPLETED] passbook={} sipId={} — all {} installments done",
                    passbookId, sip.getId(), sip.getTotalInstallments());
        } else {
            // ── ADVANCE NEXT EXECUTION DATE ───────────────────────────
            // This is the core of the scheduling system.
            // Always advance from the current nextExecutionDate (not from today),
            // so that schedule drift is avoided even if the scheduler runs late.
            LocalDate nextDate = sipService.calculateNextDateFrom(
                    sip.getNextExecutionDate(), sip.getFrequency());
            sip.setNextExecutionDate(nextDate);
            log.info("[SIP][NEXT] passbook={} sipId={} nextExec={} remaining={}",
                    passbookId, sip.getId(), nextDate, newRemaining);
        }

        sipRepository.save(sip);
        return true;
    }
}