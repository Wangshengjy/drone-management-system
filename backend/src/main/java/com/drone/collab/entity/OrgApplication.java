package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("org_application")
public class OrgApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private String applyType;
    private String status;
    private String autoResult;
    private Integer autoScore;
    private String autoReason;
    private LocalDateTime createdAt;
}
