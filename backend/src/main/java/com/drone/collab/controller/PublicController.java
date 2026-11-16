package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.common.PageResult;
import com.drone.collab.service.PublicService;
import com.drone.collab.vo.OrgPublicVo;
import com.drone.collab.vo.PilotPublicVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicService publicService;

    public PublicController(PublicService publicService) {
        this.publicService = publicService;
    }

    @GetMapping("/pilots")
    @PreAuthorize("hasAnyRole('ADMIN', 'PILOT', 'ORG')")
    public ApiResponse<PageResult<PilotPublicVo>> pilots(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(publicService.pilots(keyword, region, page, size));
    }

    @GetMapping("/organizations")
    @PreAuthorize("hasAnyRole('ADMIN', 'PILOT', 'ORG')")
    public ApiResponse<PageResult<OrgPublicVo>> organizations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orgType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(publicService.organizations(keyword, orgType, page, size));
    }
}
