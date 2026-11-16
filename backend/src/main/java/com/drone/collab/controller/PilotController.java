package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.common.PageResult;
import com.drone.collab.dto.CertificateSaveRequest;
import com.drone.collab.dto.PilotSaveRequest;
import com.drone.collab.entity.AuditLog;
import com.drone.collab.entity.Pilot;
import com.drone.collab.entity.PilotCertificate;
import com.drone.collab.security.SecurityUtils;
import com.drone.collab.service.AuditLogService;
import com.drone.collab.service.PilotService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pilots")
public class PilotController {

    private final PilotService pilotService;
    private final AuditLogService auditLogService;

    public PilotController(PilotService pilotService, AuditLogService auditLogService) {
        this.pilotService = pilotService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResult<Pilot>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(pilotService.page(keyword, region, status, page, size, SecurityUtils.requireUser()));
    }

    @GetMapping("/me")
    public ApiResponse<Pilot> me() {
        return ApiResponse.ok(pilotService.getMe(SecurityUtils.requireUser()));
    }

    @GetMapping("/me/audit-logs")
    public ApiResponse<PageResult<AuditLog>> myAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        var user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            throw new com.drone.collab.common.BusinessException(403, "非飞手账号");
        }
        return ApiResponse.ok(auditLogService.page("PILOT", user.getPilotId(), page, size));
    }

    @GetMapping("/me/status-flow")
    public ApiResponse<Map<String, Object>> myStatusFlow() {
        var user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            throw new com.drone.collab.common.BusinessException(403, "非飞手账号");
        }
        Pilot pilot = pilotService.get(user.getPilotId(), user);
        var logs = auditLogService.page("PILOT", user.getPilotId(), 1, 50);
        return ApiResponse.ok(Map.of(
                "currentStatus", pilot.getStatus() != null ? pilot.getStatus() : "DRAFT",
                "autoResult", pilot.getAutoResult() != null ? pilot.getAutoResult() : "",
                "autoScore", pilot.getAutoScore() != null ? pilot.getAutoScore() : 0,
                "auditLogs", logs.getRecords()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<Pilot> get(@PathVariable Long id) {
        return ApiResponse.ok(pilotService.get(id, SecurityUtils.requireUser()));
    }

    @PostMapping
    public ApiResponse<Pilot> create(@Valid @RequestBody PilotSaveRequest req) {
        return ApiResponse.ok(pilotService.create(req, SecurityUtils.requireUser()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Pilot> update(@PathVariable Long id, @RequestBody PilotSaveRequest req) {
        return ApiResponse.ok(pilotService.update(id, req, SecurityUtils.requireUser()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        pilotService.delete(id, SecurityUtils.requireUser());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        pilotService.submit(id, SecurityUtils.requireUser());
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/certificates")
    public ApiResponse<PageResult<PilotCertificate>> certs(@PathVariable Long id) {
        return ApiResponse.ok(pilotService.listCerts(id, SecurityUtils.requireUser()));
    }

    @PostMapping("/{id}/certificates")
    public ApiResponse<PilotCertificate> addCert(@PathVariable Long id, @RequestBody CertificateSaveRequest req) {
        return ApiResponse.ok(pilotService.addCert(id, req, SecurityUtils.requireUser()));
    }

    @PutMapping("/{pilotId}/certificates/{certId}")
    public ApiResponse<PilotCertificate> updateCert(
            @PathVariable Long pilotId,
            @PathVariable Long certId,
            @RequestBody CertificateSaveRequest req) {
        return ApiResponse.ok(pilotService.updateCert(pilotId, certId, req, SecurityUtils.requireUser()));
    }

    @DeleteMapping("/{pilotId}/certificates/{certId}")
    public ApiResponse<Void> deleteCert(@PathVariable Long pilotId, @PathVariable Long certId) {
        pilotService.deleteCert(pilotId, certId, SecurityUtils.requireUser());
        return ApiResponse.ok();
    }
}
