package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.common.BusinessException;
import com.drone.collab.common.PageResult;
import com.drone.collab.dto.OrgSaveRequest;
import com.drone.collab.entity.AuditLog;
import com.drone.collab.entity.Organization;
import com.drone.collab.security.SecurityUtils;
import com.drone.collab.service.AuditLogService;
import com.drone.collab.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final AuditLogService auditLogService;

    public OrganizationController(OrganizationService organizationService, AuditLogService auditLogService) {
        this.organizationService = organizationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResult<Organization>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orgType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(organizationService.page(keyword, orgType, status, page, size, SecurityUtils.requireUser()));
    }

    @GetMapping("/me")
    public ApiResponse<Organization> me() {
        var u = SecurityUtils.requireUser();
        if (!"ORG".equals(u.getRole()) || u.getOrgId() == null) {
            throw new BusinessException(403, "非单位账号");
        }
        return ApiResponse.ok(organizationService.get(u.getOrgId(), u));
    }

    @GetMapping("/me/audit-logs")
    public ApiResponse<PageResult<AuditLog>> myAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        var u = SecurityUtils.requireUser();
        if (!"ORG".equals(u.getRole()) || u.getOrgId() == null) {
            throw new BusinessException(403, "非单位账号");
        }
        return ApiResponse.ok(auditLogService.page("ORG", u.getOrgId(), page, size));
    }

    @GetMapping("/me/status-flow")
    public ApiResponse<Map<String, Object>> myStatusFlow() {
        var u = SecurityUtils.requireUser();
        if (!"ORG".equals(u.getRole()) || u.getOrgId() == null) {
            throw new BusinessException(403, "非单位账号");
        }
        Organization org = organizationService.get(u.getOrgId(), u);
        var logs = auditLogService.page("ORG", u.getOrgId(), 1, 50);
        return ApiResponse.ok(Map.of(
                "currentStatus", org.getStatus() != null ? org.getStatus() : "DRAFT",
                "autoResult", org.getAutoResult() != null ? org.getAutoResult() : "",
                "autoScore", org.getAutoScore() != null ? org.getAutoScore() : 0,
                "auditLogs", logs.getRecords()
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<Organization> get(@PathVariable Long id) {
        return ApiResponse.ok(organizationService.get(id, SecurityUtils.requireUser()));
    }

    @PostMapping
    public ApiResponse<Organization> create(@Valid @RequestBody OrgSaveRequest req) {
        return ApiResponse.ok(organizationService.create(req, SecurityUtils.requireUser()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Organization> update(@PathVariable Long id, @RequestBody OrgSaveRequest req) {
        return ApiResponse.ok(organizationService.update(id, req, SecurityUtils.requireUser()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        organizationService.delete(id, SecurityUtils.requireUser());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        organizationService.submit(id, SecurityUtils.requireUser());
        return ApiResponse.ok();
    }
}
