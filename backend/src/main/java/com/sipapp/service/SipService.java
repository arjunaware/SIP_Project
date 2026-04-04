package com.sipapp.service;

import com.sipapp.dto.SipDto;
import com.sipapp.dto.TransactionDto;
import com.sipapp.entity.*;
import com.sipapp.enums.Frequency;
import com.sipapp.enums.SipStatus;
import com.sipapp.enums.TransactionStatus;
import com.sipapp.exception.BadRequestException;
import com.sipapp.exception.ResourceNotFoundException;
import com.sipapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SipService {

    private static final Logger log = LoggerFactory.getLogger(SipService.class);

    @Autowired private SipRepository sipRepository;
    @Autowired private PassbookRepository passbookRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;

    @Value("${app.sip.default-interest-rate}")
    private BigDecimal defaultInterestRate;

    // ══════════════════════════════════════════════════════════════════
    // SIP CREATION
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public SipDto.SipResponse createSip(SipDto.CreateRequest request, String email) {

        // ── Validate passbook ─────────────────────────────────────────
        if (request.getPassbookId() == null || request.getPassbookId().isBlank()) {
            throw new BadRequestException("Passbook ID is mandatory");
        }

        Passbook passbook = passbookRepository.findById(request.getPassbookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Passbook not found: " + request.getPassbookId()));

        if (!passbook.getUser().getEmail().equals(email)) {
            throw new BadRequestException("Passbook does not belong to the current user");
        }

        // ── Validate installments ─────────────────────────────────────
        if (request.getTotalInstallments() < 1) {
            throw new BadRequestException("Total installments must be at least 1");
        }

        // ── Determine interest rate ───────────────────────────────────
        BigDecimal interestRate;
        if (Boolean.TRUE.equals(request.getTrust())) {
            if (request.getInterestRate() == null) {
                throw new BadRequestException("Interest rate is required when trust is true");
            }
            if (request.getInterestRate().compareTo(BigDecimal.ZERO) < 0
                    || request.getInterestRate().compareTo(new BigDecimal("15")) > 0) {
                throw new BadRequestException("Interest rate must be between 0 and 15");
            }
            interestRate = request.getInterestRate();
        } else {
            interestRate = defaultInterestRate;
        }

        // ── Build SIP ─────────────────────────────────────────────────
        LocalDate firstExecDate = request.getStartDate();

        Sip sip = Sip.builder()
                .passbook(passbook)
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .totalInstallments(request.getTotalInstallments())
                .remainingInstallments(request.getTotalInstallments())  // all remaining at start
                .completedInstallments(0)
                .interestRate(interestRate)
                .trust(request.getTrust())
                .isSip(request.getIsSip())
                .startDate(request.getStartDate())
                .nextExecutionDate(firstExecDate)   // scheduler uses this
                .status(SipStatus.ACTIVE)
                .build();

        sip = sipRepository.save(sip);

        log.info("[SIP] Created SIP id={} passbook={} frequency={} firstExec={}",
                sip.getId(), passbook.getId(), sip.getFrequency(), firstExecDate);

        return mapToResponse(sip, false);
    }

    // ══════════════════════════════════════════════════════════════════
    // PAUSE
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public SipDto.SipResponse pauseSip(Long sipId, String email) {
        Sip sip = getAndVerifySip(sipId, email);

        // ── Edge case guards ──────────────────────────────────────────
        if (sip.getStatus() == SipStatus.COMPLETED) {
            throw new BadRequestException("Cannot pause a COMPLETED SIP");
        }
        if (sip.getStatus() == SipStatus.PAUSED) {
            throw new BadRequestException("SIP is already PAUSED");
        }

        // ── Pause ─────────────────────────────────────────────────────
        sip.setStatus(SipStatus.PAUSED);
        sip.setPausedAt(LocalDate.now());
        // NOTE: we do NOT touch nextExecutionDate here.
        // When resumed, the service will recalculate from today.
        sipRepository.save(sip);

        log.info("[SIP] PAUSED sip={} passbook={} at={} nextWas={}",
                sip.getId(), sip.getPassbook().getId(),
                LocalDate.now(), sip.getNextExecutionDate());

        return mapToResponse(sip, false);
    }

    // ══════════════════════════════════════════════════════════════════
    // RESUME
    // ══════════════════════════════════════════════════════════════════

    @Transactional
    public SipDto.SipResponse resumeSip(Long sipId, String email) {
        Sip sip = getAndVerifySip(sipId, email);

        // ── Edge case guards ──────────────────────────────────────────
        if (sip.getStatus() == SipStatus.COMPLETED) {
            throw new BadRequestException("Cannot resume a COMPLETED SIP");
        }
        if (sip.getStatus() == SipStatus.ACTIVE) {
            throw new BadRequestException("SIP is already ACTIVE");
        }

        // ── Recalculate next execution date from TODAY ─────────────────
        // CRITICAL: do NOT restart from the original startDate.
        // The user has completed some installments already.
        // Resume means: next installment runs after 1 frequency gap from now.
        LocalDate newNextExecDate = calculateNextDateFrom(LocalDate.now(), sip.getFrequency());

        sip.setStatus(SipStatus.ACTIVE);
        sip.setResumedAt(LocalDate.now());
        sip.setNextExecutionDate(newNextExecDate);
        sipRepository.save(sip);

        log.info("[SIP] RESUMED sip={} passbook={} at={} nextExec={}",
                sip.getId(), sip.getPassbook().getId(), LocalDate.now(), newNextExecDate);

        return mapToResponse(sip, false);
    }

    // ══════════════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════════════

    public List<SipDto.SipResponse> getAllSipsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return sipRepository.findAllByUserId(user.getId())
                .stream()
                .map(sip -> mapToResponse(sip, false))
                .collect(Collectors.toList());
    }

    public SipDto.SipResponse getSipById(Long sipId, String email) {
        Sip sip = getAndVerifySip(sipId, email);
        return mapToResponse(sip, true);
    }

    // ══════════════════════════════════════════════════════════════════
    // INTEREST CALCULATION
    // ══════════════════════════════════════════════════════════════════

    /**
     * Compound interest per installment.
     * Formula: Interest = P × ((1 + r)^n − 1)
     *   P = installment amount
     *   r = annual rate / 100
     *   n = installment number
     */
    public BigDecimal calculateInterestForInstallment(BigDecimal principal,
                                                      BigDecimal annualRate,
                                                      int installmentNumber) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal r = annualRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal compoundFactor = BigDecimal.ONE.add(r)
                .pow(installmentNumber, new MathContext(15, RoundingMode.HALF_UP));
        return principal.multiply(compoundFactor)
                .subtract(principal)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Build a Transaction entity (not saved — caller saves it).
     * Used by both SipService (on create) and SipScheduler.
     */
    public Transaction buildTransaction(Sip sip, int installmentNumber, LocalDate date) {
        BigDecimal interest = calculateInterestForInstallment(
                sip.getAmount(), sip.getInterestRate(), installmentNumber);
        BigDecimal totalAmount = sip.getAmount().add(interest).setScale(2, RoundingMode.HALF_UP);

        return Transaction.builder()
                .sip(sip)
                .installmentNumber(installmentNumber)
                .amount(sip.getAmount())
                .interest(interest)
                .totalAmount(totalAmount)
                .transactionDate(date)
                .status(TransactionStatus.PENDING)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // DATE CALCULATION  (used by scheduler and resume)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Given a base date, calculates the next execution date for a frequency.
     * Java's LocalDate handles Feb 28/29 edge cases automatically.
     */
    public LocalDate calculateNextDateFrom(LocalDate base, Frequency frequency) {
        return switch (frequency) {
            case DAILY   -> base.plusDays(1);
            case WEEKLY  -> base.plusWeeks(1);
            case MONTHLY -> base.plusMonths(1);
            case YEARLY  -> base.plusYears(1);
        };
    }

    // ══════════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ══════════════════════════════════════════════════════════════════

    private Sip getAndVerifySip(Long sipId, String email) {
        Sip sip = sipRepository.findById(sipId)
                .orElseThrow(() -> new ResourceNotFoundException("SIP not found: " + sipId));
        if (!sip.getPassbook().getUser().getEmail().equals(email)) {
            throw new BadRequestException("Access denied to this SIP");
        }
        return sip;
    }

    public SipDto.SipResponse mapToResponse(Sip sip, boolean includeTransactions) {
        List<Transaction> transactions = transactionRepository
                .findBySipIdOrderByInstallmentNumberAsc(sip.getId());

        long completed = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();

        BigDecimal totalContribution = sip.getAmount()
                .multiply(BigDecimal.valueOf(completed))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalInterest = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getInterest)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        SipDto.SipResponse res = new SipDto.SipResponse();
        res.setId(sip.getId());
        res.setPassbookId(sip.getPassbook().getId());
        res.setAmount(sip.getAmount());
        res.setFrequency(sip.getFrequency());
        res.setTotalInstallments(sip.getTotalInstallments());
        res.setCompletedInstallments(sip.getCompletedInstallments());
        res.setRemainingInstallments(sip.getRemainingInstallments());
        res.setInterestRate(sip.getInterestRate());
        res.setTrust(sip.getTrust());
        res.setIsSip(sip.getIsSip());
        res.setStartDate(sip.getStartDate());
        res.setNextExecutionDate(sip.getNextExecutionDate());
        res.setStatus(sip.getStatus());
        res.setPausedAt(sip.getPausedAt());
        res.setResumedAt(sip.getResumedAt());
        res.setTotalContribution(totalContribution);
        res.setTotalInterest(totalInterest);
        res.setCurrentAmount(totalContribution.add(totalInterest));

        if (includeTransactions) {
            res.setTransactions(transactions.stream().map(t -> {
                TransactionDto.Response tr = new TransactionDto.Response();
                tr.setId(t.getId());
                tr.setInstallmentNumber(t.getInstallmentNumber());
                tr.setAmount(t.getAmount());
                tr.setInterest(t.getInterest());
                tr.setTotalAmount(t.getTotalAmount());
                tr.setTransactionDate(t.getTransactionDate());
                tr.setStatus(t.getStatus());
                return tr;
            }).collect(Collectors.toList()));
        }

        return res;
    }
}