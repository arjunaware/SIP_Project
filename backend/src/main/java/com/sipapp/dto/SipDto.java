package com.sipapp.dto;

import com.sipapp.enums.Frequency;
import com.sipapp.enums.SipStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SipDto {

    // ── Create request (unchanged) ────────────────────────────────────
    @Data
    public static class CreateRequest {

        @NotBlank(message = "Passbook ID is mandatory")
        private String passbookId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.0", message = "Amount must be at least 1")
        private BigDecimal amount;

        @NotNull(message = "Frequency is required")
        private Frequency frequency;

        @NotNull(message = "Total installments is required")
        @Min(value = 1, message = "Total installments must be at least 1")
        private Integer totalInstallments;

        @NotNull(message = "Trust flag is required")
        private Boolean trust;

        private BigDecimal interestRate;

        @NotNull(message = "isSip flag is required")
        private Boolean isSip;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;
    }

    // ── Full SIP response (new fields added) ──────────────────────────
    @Data
    public static class SipResponse {
        private Long id;
        private String passbookId;
        private BigDecimal amount;
        private Frequency frequency;

        // Installment tracking
        private Integer totalInstallments;
        private Integer completedInstallments;   // ← new
        private Integer remainingInstallments;   // ← new

        // Financial summary
        private BigDecimal interestRate;
        private Boolean trust;
        private Boolean isSip;
        private BigDecimal totalContribution;
        private BigDecimal totalInterest;
        private BigDecimal currentAmount;

        // Scheduling
        private LocalDate startDate;
        private LocalDate nextExecutionDate;     // ← new

        // Status
        private SipStatus status;                // ACTIVE / PAUSED / COMPLETED
        private LocalDate pausedAt;              // ← new
        private LocalDate resumedAt;             // ← new

        // Transactions (only in detail view)
        private List<TransactionDto.Response> transactions;
    }
}