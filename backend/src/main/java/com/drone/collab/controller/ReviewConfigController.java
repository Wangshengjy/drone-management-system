package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.common.BusinessException;
import com.drone.collab.security.SecurityUtils;
import com.drone.collab.service.ReviewConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/review-config")
public class ReviewConfigController {

    private final ReviewConfigService reviewConfigService;
    private final ObjectMapper objectMapper;

    public ReviewConfigController(ReviewConfigService reviewConfigService, ObjectMapper objectMapper) {
        this.reviewConfigService = reviewConfigService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> get() {
        requireAdmin();
        return ApiResponse.ok(reviewConfigService.getDefaultConfigMap());
    }

    @PutMapping
    public ApiResponse<Void> save(@RequestBody Map<String, Object> body) throws JsonProcessingException {
        requireAdmin();
        if (body == null || body.isEmpty()) {
            throw new BusinessException("配置不能为空");
        }
        reviewConfigService.saveDefault(objectMapper.writeValueAsString(body));
        return ApiResponse.ok();
    }

    private static void requireAdmin() {
        var u = SecurityUtils.requireUser();
        if (!"ADMIN".equals(u.getRole())) {
            throw new BusinessException(403, "仅管理员");
        }
    }
}
