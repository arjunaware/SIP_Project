package com.sipapp.entity;

import com.sipapp.enums.Frequency;
import com.sipapp.enums.SipStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "sips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passbook_id", nullable = false)
    private Passbook passbook;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "total_installments", nullable = false)
    private Integer totalInstallments;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Boolean trust;

    @Column(name = "is_sip", nullable = false)
    private Boolean isSip;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SipStatus status = SipStatus.ACTIVE;

    @OneToMany(mappedBy = "sip", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;
}
