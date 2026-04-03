package com.sipapp.service;

import com.sipapp.dto.SipDto;
import com.sipapp.dto.TransactionDto;
import com.sipapp.entity.*;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SipService {

    private static final Logger logger = LoggerFactory.getLogger(SipService.class);

    @Autowired private SipRepository sipRepository;
    @Autowired private PassbookRepository passbookRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;

    @Value("${app.sip.default-interest-rate}")
    private BigDecimal defaultInterestRate;

    @Transactional
    public SipDto.SipResponse createSip(SipDto.CreateRequest request, String email) {
        // Validate passbook
        if (request.getPassbookId() == null || request.getPassbookId().isBlank()) {
            throw new BadRequestException("Passbook ID is mandatory");
        }

        Passbook passbook = passbookRepository.findById(request.getPassbookId())
                .orElseThrow(() -> new ResourceNotFoundException("Passbook not found: " + request.getPassbookId()));

        // Ensure passbook belongs to current user
        if (!passbook.getUser().getEmail().equals(email)) {
            throw new BadRequestException("Passbook does not belong to current user");
        }

        // Validate total installments
        if (request.getTotalInstallments() < 1) {
            throw new BadRequestException("Total installments must be at least 1");
        }

        // Determine interest rate
        BigDecimal interestRate;
        if (Boolean.TRUE.equals(request.getTrust())) {
            if (request.getInterestRate() == null) {
                throw new BadRequestException("Interest rate is required when trust is true");
            }
            if (request.getInterestRate().compareTo(BigDecimal.ZERO) < 0
                    || request.getInterestRate().compareTo(new BigDecimal("15")) > 0) {
                throw new BadRequestException("Interest rate must be between 0 and 15 when trust is true");
            }
            interestRate = request.getInterestRate();
        } else {
            interestRate = defaultInterestRate;
        }

        // Build and save SIP
        Sip sip = Sip.builder()
                .passbook(passbook)
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .totalInstallments(request.getTotalInstallments())
                .interestRate(interestRate)
                .trust(request.getTrust())
                .isSip(request.getIsSip())
                .startDate(request.getStartDate())
                .status(SipStatus.ACTIVE)
                .build();

        sip = sipRepository.save(sip);

        // Create first transaction only
        Transaction firstTransaction = buildTransaction(sip, 1, sip.getStartDate());
        transactionRepository.save(firstTransaction);

        logger.info("[SIP] {} → Installment 1 created for SIP ID: {}",
                passbook.getId(), sip.getId());

        return mapToResponse(sip, false);
    }

    public List<SipDto.SipResponse> getAllSipsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return sipRepository.findAllByUserId(user.getId())
                .stream()
                .map(sip -> mapToResponse(sip, false))
                .collect(Collectors.toList());
    }

    public SipDto.SipResponse getSipById(Long sipId, String email) {
        Sip sip = sipRepository.findById(sipId)
                .orElseThrow(() -> new ResourceNotFoundException("SIP not found: " + sipId));

        // Security check
        if (!sip.getPassbook().getUser().getEmail().equals(email)) {
            throw new BadRequestException("Access denied to this SIP");
        }

        return mapToResponse(sip, true);
    }

    // ───── Interest calculation (compound per installment) ─────
    // Formula: Interest = P × ((1 + r/n)^(n × t) - 1)
    // Where P = installment amount, r = annual rate, n = frequency per year, t = time in years
    public BigDecimal calculateInterestForInstallment(BigDecimal principal, BigDecimal annualRate,
                                                       int installmentNumber) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Use compound interest: A = P(1 + r)^n - P  where r is per-installment rate
        BigDecimal r = annualRate.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal base = BigDecimal.ONE.add(r);
        BigDecimal compoundFactor = base.pow(installmentNumber, new MathContext(15, RoundingMode.HALF_UP));
        BigDecimal totalAmount = principal.multiply(compoundFactor).setScale(2, RoundingMode.HALF_UP);
        return totalAmount.subtract(principal).setScale(2, RoundingMode.HALF_UP);
    }

    public Transaction buildTransaction(Sip sip, int installmentNumber, java.time.LocalDate date) {
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

    // ───── Mapper ─────
    public SipDto.SipResponse mapToResponse(Sip sip, boolean includeTransactions) {
        List<Transaction> transactions = transactionRepository
                .findBySipIdOrderByInstallmentNumberAsc(sip.getId());

        long completed = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();
        long pending = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PENDING).count();

        BigDecimal totalContribution = sip.getAmount()
                .multiply(BigDecimal.valueOf(completed))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalInterest = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getInterest)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal currentAmount = totalContribution.add(totalInterest);

        SipDto.SipResponse response = new SipDto.SipResponse();
        response.setId(sip.getId());
        response.setPassbookId(sip.getPassbook().getId());
        response.setAmount(sip.getAmount());
        response.setFrequency(sip.getFrequency());
        response.setTotalInstallments(sip.getTotalInstallments());
        response.setCompletedInstallments((int) completed);
        response.setPendingInstallments((int) pending);
        response.setInterestRate(sip.getInterestRate());
        response.setTrust(sip.getTrust());
        response.setIsSip(sip.getIsSip());
        response.setStartDate(sip.getStartDate());
        response.setStatus(sip.getStatus());
        response.setTotalContribution(totalContribution);
        response.setTotalInterest(totalInterest);
        response.setCurrentAmount(currentAmount);

        if (includeTransactions) {
            response.setTransactions(transactions.stream().map(t -> {
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

        return response;
    }
}
