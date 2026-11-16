package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.entity.Notification;
import com.drone.collab.security.SecurityUtils;
import com.drone.collab.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<Notification>> getMyNotifications() {
        Long userId = SecurityUtils.requireUser().getUserId();
        return ApiResponse.ok(notificationService.getNotifications(userId));
    }

    @GetMapping("/unread")
    public ApiResponse<List<Notification>> getUnreadNotifications() {
        Long userId = SecurityUtils.requireUser().getUserId();
        return ApiResponse.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount() {
        Long userId = SecurityUtils.requireUser().getUserId();
        return ApiResponse.ok(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        Long userId = SecurityUtils.requireUser().getUserId();
        notificationService.markAllAsRead(userId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ApiResponse.ok(null);
    }
}
