package com.sipapp.service;

import com.sipapp.dto.DashboardDto;
import com.sipapp.dto.SipDto;
import com.sipapp.entity.User;
import com.sipapp.enums.SipStatus;
import com.sipapp.exception.ResourceNotFoundException;
import com.sipapp.repository.SipRepository;
import com.sipapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DashboardService {

    @Autowired private UserRepository userRepository;
    @Autowired private SipRepository sipRepository;
    @Autowired private SipService sipService;

    public DashboardDto getDashboard(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<SipDto.SipResponse> sipResponses = sipRepository.findAllByUserId(user.getId())
                .stream()
                .map(sip -> sipService.mapToResponse(sip, false))
                .toList();

        BigDecimal totalContribution = sipResponses.stream()
                .map(SipDto.SipResponse::getTotalContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalInterest = sipResponses.stream()
                .map(SipDto.SipResponse::getTotalInterest)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal currentAmount = totalContribution.add(totalInterest)
                .setScale(2, RoundingMode.HALF_UP);

        long active = sipResponses.stream()
                .filter(s -> s.getStatus() == SipStatus.ACTIVE).count();
        long completed = sipResponses.stream()
                .filter(s -> s.getStatus() == SipStatus.COMPLETED).count();

        DashboardDto dashboard = new DashboardDto();
        dashboard.setTotalContribution(totalContribution);
        dashboard.setTotalInterest(totalInterest);
        dashboard.setCurrentAmount(currentAmount);
        dashboard.setTotalSips(sipResponses.size());
        dashboard.setActiveSips((int) active);
        dashboard.setCompletedSips((int) completed);
        dashboard.setSips(sipResponses);

        return dashboard;
    }
}
