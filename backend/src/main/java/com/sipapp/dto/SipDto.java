package com.sipapp.dto;

import com.sipapp.enums.Frequency;
import com.sipapp.enums.SipStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SipDto {

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

        // Only required/validated if trust = true (handled in service)
        private BigDecimal interestRate;

        @NotNull(message = "isSip flag is required")
        private Boolean isSip;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;
    }

    @Data
    public static class SipResponse {
        private Long id;
        private String passbookId;
        private BigDecimal amount;
        private Frequency frequency;
        private Integer totalInstallments;
        private Integer completedInstallments;
        private Integer pendingInstallments;
        private BigDecimal interestRate;
        private Boolean trust;
        private Boolean isSip;
        private LocalDate startDate;
        private SipStatus status;
        private BigDecimal totalContribution;
        private BigDecimal totalInterest;
        private BigDecimal currentAmount;
        private List<TransactionDto.Response> transactions;
    }
}
