package com.sipapp.controller;

import com.sipapp.dto.DashboardDto;
import com.sipapp.dto.SipDto;
import com.sipapp.service.DashboardService;
import com.sipapp.service.SipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "SIP", description = "SIP Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class SipController {

    @Autowired private SipService sipService;
    @Autowired private DashboardService dashboardService;

    // ── Create ────────────────────────────────────────────────────────

    @PostMapping("/sip/create")
    @Operation(summary = "Create a new SIP")
    public ResponseEntity<SipDto.SipResponse> createSip(
            @Valid @RequestBody SipDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sipService.createSip(request, userDetails.getUsername()));
    }

    // ── Read ──────────────────────────────────────────────────────────

    @GetMapping("/sip/all")
    @Operation(summary = "Get all SIPs for the authenticated user")
    public ResponseEntity<List<SipDto.SipResponse>> getAllSips(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(sipService.getAllSipsForUser(userDetails.getUsername()));
    }

    @GetMapping("/sip/{sipId}")
    @Operation(summary = "Get SIP details with all transactions")
    public ResponseEntity<SipDto.SipResponse> getSipById(
            @PathVariable Long sipId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(sipService.getSipById(sipId, userDetails.getUsername()));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard summary")
    public ResponseEntity<DashboardDto> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getDashboard(userDetails.getUsername()));
    }

    // ── Pause / Resume ────────────────────────────────────────────────

    @PutMapping("/sip/{sipId}/pause")
    @Operation(summary = "Pause an active SIP — stops scheduler from executing it")
    public ResponseEntity<SipDto.SipResponse> pauseSip(
            @PathVariable Long sipId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(sipService.pauseSip(sipId, userDetails.getUsername()));
    }

    @PutMapping("/sip/{sipId}/resume")
    @Operation(summary = "Resume a paused SIP — continues from remaining installments")
    public ResponseEntity<SipDto.SipResponse> resumeSip(
            @PathVariable Long sipId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(sipService.resumeSip(sipId, userDetails.getUsername()));
    }
}