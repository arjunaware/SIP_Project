package com.sipapp.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardDto {
    private BigDecimal totalContribution;
    private BigDecimal totalInterest;
    private BigDecimal currentAmount;
    private int totalSips;
    private int activeSips;
    private int completedSips;
    private List<SipDto.SipResponse> sips;
}
