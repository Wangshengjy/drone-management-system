package com.drone.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.drone.collab.entity.Notification;
import com.drone.collab.entity.SysUser;
import com.drone.collab.mapper.NotificationMapper;
import com.drone.collab.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final SysUserMapper userMapper;

    @Transactional
    public void sendMissionNotification(Long missionId, String title, String content) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getRole, "PILOT", "ORG")
                .eq(SysUser::getEnabled, 1));
        
        for (SysUser user : users) {
            if (user.getPilotId() == null && user.getOrgId() == null) {
                continue;
            }
            Notification n = new Notification();
            n.setUserId(user.getId());
            n.setTargetType("MISSION");
            n.setTargetId(missionId);
            n.setTitle(title);
            n.setContent(content);
            n.setType("MISSION");
            n.setReadFlag(false);
            n.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(n);
        }
    }

    @Transactional
    public void sendToUser(Long userId, String targetType, Long targetId, String title, String content, String type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTargetType(targetType);
        n.setTargetId(targetId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setReadFlag(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationMapper.findByUserId(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationMapper.findUnreadByUserId(userId);
    }

    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification n = notificationMapper.selectById(notificationId);
        if (n != null) {
            n.setReadFlag(true);
            notificationMapper.updateById(n);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationMapper.findUnreadByUserId(userId);
        for (Notification n : unread) {
            n.setReadFlag(true);
            notificationMapper.updateById(n);
        }
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        notificationMapper.deleteById(notificationId);
    }
}
