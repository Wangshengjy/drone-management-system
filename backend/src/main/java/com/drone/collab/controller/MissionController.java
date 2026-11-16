package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.common.PageResult;
import com.drone.collab.entity.*;
import com.drone.collab.security.SecurityUser;
import com.drone.collab.security.SecurityUtils;
import com.drone.collab.service.MissionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @PostMapping
    public ApiResponse<Mission> create(@RequestBody Mission mission) {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"ADMIN".equals(user.getRole())) {
            return ApiResponse.fail(403, "仅管理员可创建任务");
        }
        return ApiResponse.ok(missionService.create(mission, user));
    }

    @GetMapping
    public ApiResponse<PageResult<Mission>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String missionType,
            @RequestParam(required = false) String urgency,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(missionService.page(status, missionType, urgency, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Mission> get(@PathVariable Long id) {
        return ApiResponse.ok(missionService.getById(id));
    }

    @GetMapping("/{id}/assignments")
    public ApiResponse<List<MissionAssignment>> assignments(@PathVariable Long id) {
        return ApiResponse.ok(missionService.getAssignments(id));
    }

    @GetMapping("/{id}/assignment-detail")
    public ApiResponse<List<Map<String, Object>>> assignmentDetail(@PathVariable Long id) {
        return ApiResponse.ok(missionService.getAssignmentDetail(id));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<MissionLog>> logs(@PathVariable Long id) {
        return ApiResponse.ok(missionService.getLogs(id));
    }

    @PostMapping("/{id}/dispatch")
    public ApiResponse<List<MissionAssignment>> dispatch(@PathVariable Long id) {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"ADMIN".equals(user.getRole())) {
            return ApiResponse.fail(403, "仅管理员可派单");
        }
        return ApiResponse.ok(missionService.smartDispatch(id, user));
    }

    @PostMapping("/{id}/manual-dispatch")
    public ApiResponse<MissionAssignment> manualDispatch(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"ADMIN".equals(user.getRole())) {
            return ApiResponse.fail(403, "仅管理员可派单");
        }
        String assigneeType = (String) body.get("assigneeType");
        Long assigneeId = ((Number) body.get("assigneeId")).longValue();
        return ApiResponse.ok(missionService.manualDispatch(id, user, assigneeType, assigneeId));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<Mission> accept(@PathVariable Long id) {
        SecurityUser user = SecurityUtils.requireUser();
        return ApiResponse.ok(missionService.acceptAssignment(id, user));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Mission> reject(@PathVariable Long id) {
        SecurityUser user = SecurityUtils.requireUser();
        return ApiResponse.ok(missionService.rejectAssignment(id, user));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Mission> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        SecurityUser user = SecurityUtils.requireUser();
        String newStatus = body.get("status");
        String comment = body.get("comment");
        String attachmentUrl = body.get("attachmentUrl");
        return ApiResponse.ok(missionService.updateStatus(id, newStatus, user, comment, attachmentUrl));
    }

    @GetMapping("/my")
    public ApiResponse<List<Mission>> myMissions() {
        SecurityUser user = SecurityUtils.requireUser();
        return ApiResponse.ok(missionService.myMissions(user));
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(missionService.getDashboardData());
    }

    @GetMapping("/pilot-status/{pilotId}")
    public ApiResponse<PilotStatus> getPilotStatus(@PathVariable Long pilotId) {
        return ApiResponse.ok(missionService.getPilotStatus(pilotId));
    }

    @GetMapping("/org-status/{orgId}")
    public ApiResponse<OrgStatus> getOrgStatus(@PathVariable Long orgId) {
        return ApiResponse.ok(missionService.getOrgStatus(orgId));
    }

    @PutMapping("/pilot-status")
    public ApiResponse<PilotStatus> updatePilotStatus(@RequestBody Map<String, Object> body) {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            return ApiResponse.fail(403, "非飞手账号");
        }
        return ApiResponse.ok(missionService.updatePilotStatus(
                user.getPilotId(),
                (String) body.get("availability"),
                body.get("latitude") != null ? ((Number) body.get("latitude")).doubleValue() : null,
                body.get("longitude") != null ? ((Number) body.get("longitude")).doubleValue() : null,
                (String) body.get("droneType")));
    }

    @PutMapping("/org-status")
    public ApiResponse<OrgStatus> updateOrgStatus(@RequestBody Map<String, Object> body) {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            return ApiResponse.fail(403, "非单位账号");
        }
        return ApiResponse.ok(missionService.updateOrgStatus(
                user.getOrgId(),
                (String) body.get("availability"),
                body.get("latitude") != null ? ((Number) body.get("latitude")).doubleValue() : null,
                body.get("longitude") != null ? ((Number) body.get("longitude")).doubleValue() : null));
    }

    @GetMapping("/pilot-status/me")
    public ApiResponse<PilotStatus> myPilotStatus() {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"PILOT".equals(user.getRole()) || user.getPilotId() == null) {
            return ApiResponse.fail(403, "非飞手账号");
        }
        return ApiResponse.ok(missionService.getPilotStatus(user.getPilotId()));
    }

    @GetMapping("/org-status/me")
    public ApiResponse<OrgStatus> myOrgStatus() {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            return ApiResponse.fail(403, "非单位账号");
        }
        return ApiResponse.ok(missionService.getOrgStatus(user.getOrgId()));
    }

    @PostMapping("/check-timeout")
    public ApiResponse<Void> checkTimeout() {
        missionService.checkTimeout();
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/org-dispatch-pilot")
    public ApiResponse<MissionAssignment> orgDispatchPilot(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            return ApiResponse.fail(403, "非单位账号");
        }
        Long pilotId = ((Number) body.get("pilotId")).longValue();
        return ApiResponse.ok(missionService.orgDispatchPilot(id, user.getOrgId(), pilotId, user));
    }

    @PostMapping("/{id}/org-auto-dispatch")
    public ApiResponse<MissionAssignment> orgAutoDispatch(@PathVariable Long id) {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            return ApiResponse.fail(403, "非单位账号");
        }
        return ApiResponse.ok(missionService.orgAutoDispatchPilot(id, user.getOrgId(), user));
    }

    @GetMapping("/org-available-pilots")
    public ApiResponse<List<Map<String, Object>>> orgAvailablePilots() {
        SecurityUser user = SecurityUtils.requireUser();
        if (!"ORG".equals(user.getRole()) || user.getOrgId() == null) {
            return ApiResponse.fail(403, "非单位账号");
        }
        return ApiResponse.ok(missionService.getOrgAvailablePilots(user.getOrgId()));
    }
}
