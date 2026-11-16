package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** PILOT, ORG */
    private String entityType;
    private Long entityId;
    private String action;
    private Long operatorUserId;
    private String commentText;
    private String detailJson;
    private LocalDateTime createdAt;
}
