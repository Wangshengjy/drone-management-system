package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.common.BusinessException;
import com.drone.collab.common.PageResult;
import com.drone.collab.entity.AuditLog;
import com.drone.collab.security.SecurityUtils;
import com.drone.collab.service.AuditLogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResult<AuditLog>> page(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var u = SecurityUtils.requireUser();
        if (!"ADMIN".equals(u.getRole())) {
            throw new BusinessException(403, "仅管理员");
        }
        return ApiResponse.ok(auditLogService.page(entityType, entityId, page, size));
    }
}
