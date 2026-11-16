package com.drone.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.drone.collab.common.PageResult;
import com.drone.collab.entity.AuditLog;
import com.drone.collab.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(String entityType, Long entityId, String action, Long operatorUserId, String comment, String detailJson) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setOperatorUserId(operatorUserId);
        log.setCommentText(comment);
        log.setDetailJson(detailJson);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    public PageResult<AuditLog> page(String entityType, Long entityId, int page, int size) {
        Page<AuditLog> p = new Page<>(page, size);
        LambdaQueryWrapper<AuditLog> q = new LambdaQueryWrapper<AuditLog>()
                .eq(entityType != null && !entityType.isEmpty(), AuditLog::getEntityType, entityType)
                .eq(entityId != null, AuditLog::getEntityId, entityId)
                .orderByDesc(AuditLog::getCreatedAt);
        Page<AuditLog> r = auditLogMapper.selectPage(p, q);
        return new PageResult<>(r.getTotal(), r.getRecords());
    }
}
