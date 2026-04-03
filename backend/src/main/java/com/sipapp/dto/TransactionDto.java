package com.sipapp.dto;

import com.sipapp.enums.TransactionStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDto {

    @Data
    public static class Response {
        private Long id;
        private Integer installmentNumber;
        private BigDecimal amount;
        private BigDecimal interest;
        private BigDecimal totalAmount;
        private LocalDate transactionDate;
        private TransactionStatus status;
    }
}
