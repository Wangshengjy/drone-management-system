package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("org_status")
public class OrgStatus {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private String availability;
    private Double latitude;
    private Double longitude;
    private Integer missionCount;
    private Long activeMissionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
