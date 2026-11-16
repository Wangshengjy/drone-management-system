package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.common.BusinessException;
import com.drone.collab.entity.PilotOrg;
import com.drone.collab.security.SecurityUtils;
import com.drone.collab.service.PilotOrgService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pilot-org")
public class PilotOrgController {

    private final PilotOrgService pilotOrgService;

    public PilotOrgController(PilotOrgService pilotOrgService) {
        this.pilotOrgService = pilotOrgService;
    }

    @PostMapping("/invite")
    public ApiResponse<PilotOrg> invite(@RequestBody Map<String, Long> body) {
        var user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            throw new BusinessException(403, "非单位账号");
        }
        Long pilotId = body.get("pilotId");
        if (pilotId == null) {
            throw new BusinessException(400, "pilotId不能为空");
        }
        return ApiResponse.ok(pilotOrgService.invitePilot(user.getOrgId(), pilotId));
    }

    @PostMapping("/apply")
    public ApiResponse<PilotOrg> apply(@RequestBody Map<String, Long> body) {
        var user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            throw new BusinessException(403, "非飞手账号");
        }
        Long orgId = body.get("orgId");
        if (orgId == null) {
            throw new BusinessException(400, "orgId不能为空");
        }
        return ApiResponse.ok(pilotOrgService.applyToJoin(user.getPilotId(), orgId));
    }

    @PostMapping("/accept-invitation/{id}")
    public ApiResponse<Void> acceptInvitation(@PathVariable Long id) {
        var user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            throw new BusinessException(403, "非飞手账号");
        }
        pilotOrgService.acceptInvitation(user.getPilotId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/reject-invitation/{id}")
    public ApiResponse<Void> rejectInvitation(@PathVariable Long id) {
        var user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            throw new BusinessException(403, "非飞手账号");
        }
        pilotOrgService.rejectInvitation(user.getPilotId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/accept-application/{id}")
    public ApiResponse<Void> acceptApplication(@PathVariable Long id) {
        var user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            throw new BusinessException(403, "非单位账号");
        }
        pilotOrgService.acceptApplication(user.getOrgId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/reject-application/{id}")
    public ApiResponse<Void> rejectApplication(@PathVariable Long id) {
        var user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            throw new BusinessException(403, "非单位账号");
        }
        pilotOrgService.rejectApplication(user.getOrgId(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/leave")
    public ApiResponse<Void> leave(@RequestBody Map<String, Long> body) {
        var user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            throw new BusinessException(403, "非飞手账号");
        }
        Long orgId = body.get("orgId");
        if (orgId == null) {
            throw new BusinessException(400, "orgId不能为空");
        }
        pilotOrgService.leaveOrg(user.getPilotId(), orgId);
        return ApiResponse.ok();
    }

    @PostMapping("/remove-pilot")
    public ApiResponse<Void> removePilot(@RequestBody Map<String, Long> body) {
        var user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            throw new BusinessException(403, "非单位账号");
        }
        Long pilotId = body.get("pilotId");
        if (pilotId == null) {
            throw new BusinessException(400, "pilotId不能为空");
        }
        pilotOrgService.removePilot(user.getOrgId(), pilotId);
        return ApiResponse.ok();
    }

    @GetMapping("/org-pilots")
    public ApiResponse<List<PilotOrg>> orgPilots() {
        var user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            throw new BusinessException(403, "非单位账号");
        }
        return ApiResponse.ok(pilotOrgService.getOrgPilots(user.getOrgId()));
    }

    @GetMapping("/my-org")
    public ApiResponse<PilotOrg> myOrg() {
        var user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            throw new BusinessException(403, "非飞手账号");
        }
        return ApiResponse.ok(pilotOrgService.getPilotOrg(user.getPilotId()));
    }

    @GetMapping("/pending-invitations")
    public ApiResponse<List<PilotOrg>> pendingInvitations() {
        var user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            throw new BusinessException(403, "非飞手账号");
        }
        return ApiResponse.ok(pilotOrgService.getPendingForPilot(user.getPilotId()));
    }

    @GetMapping("/pending-applications")
    public ApiResponse<List<PilotOrg>> pendingApplications() {
        var user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            throw new BusinessException(403, "非单位账号");
        }
        return ApiResponse.ok(pilotOrgService.getPendingForOrg(user.getOrgId()));
    }
}
