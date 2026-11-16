package com.drone.collab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.collab.common.ApiResponse;
import com.drone.collab.common.BusinessException;
import com.drone.collab.common.PageResult;
import com.drone.collab.domain.Statuses;
import com.drone.collab.dto.ManualReviewRequest;
import com.drone.collab.entity.Organization;
import com.drone.collab.entity.Pilot;
import com.drone.collab.mapper.OrganizationMapper;
import com.drone.collab.mapper.PilotMapper;
import com.drone.collab.security.SecurityUtils;
import com.drone.collab.service.ReviewService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/review")
public class ReviewController {

    private final PilotMapper pilotMapper;
    private final OrganizationMapper organizationMapper;
    private final ReviewService reviewService;

    public ReviewController(
            PilotMapper pilotMapper,
            OrganizationMapper organizationMapper,
            ReviewService reviewService) {
        this.pilotMapper = pilotMapper;
        this.organizationMapper = organizationMapper;
        this.reviewService = reviewService;
    }

    @GetMapping("/pilots/pending")
    public ApiResponse<PageResult<Pilot>> pendingPilots(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireAdmin();
        Page<Pilot> p = pilotMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Pilot>()
                        .eq(Pilot::getStatus, Statuses.PENDING_REVIEW)
                        .orderByAsc(Pilot::getUpdatedAt));
        return ApiResponse.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @GetMapping("/organizations/pending")
    public ApiResponse<PageResult<Organization>> pendingOrgs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireAdmin();
        Page<Organization> p = organizationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Organization>()
                        .eq(Organization::getStatus, Statuses.PENDING_REVIEW)
                        .orderByAsc(Organization::getUpdatedAt));
        return ApiResponse.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    @PostMapping("/pilots/{id}")
    public ApiResponse<Void> reviewPilot(@PathVariable Long id, @Valid @RequestBody ManualReviewRequest req) {
        reviewService.reviewPilot(id, req, SecurityUtils.requireUser());
        return ApiResponse.ok();
    }

    @PostMapping("/organizations/{id}")
    public ApiResponse<Void> reviewOrg(@PathVariable Long id, @Valid @RequestBody ManualReviewRequest req) {
        reviewService.reviewOrg(id, req, SecurityUtils.requireUser());
        return ApiResponse.ok();
    }

    @PostMapping("/pilots/{id}/freeze")
    public ApiResponse<Void> freezePilot(@PathVariable Long id) {
        reviewService.freezePilot(id, SecurityUtils.requireUser());
        return ApiResponse.ok();
    }

    @PostMapping("/organizations/{id}/freeze")
    public ApiResponse<Void> freezeOrg(@PathVariable Long id) {
        reviewService.freezeOrg(id, SecurityUtils.requireUser());
        return ApiResponse.ok();
    }

    private static void requireAdmin() {
        var u = SecurityUtils.requireUser();
        if (!"ADMIN".equals(u.getRole())) {
            throw new BusinessException(403, "仅管理员");
        }
    }
}
