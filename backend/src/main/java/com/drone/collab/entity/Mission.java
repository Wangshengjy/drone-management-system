package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mission")
public class Mission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String missionType;
    private String urgency;
    private String status;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private String weatherInfo;
    private String requiredCertTypes;
    private String requiredDroneType;
    private Long creatorId;
    private Long assignedOrgId;
    private Long assignedPilotId;
    private Integer requiredPilotCount;
    private Integer requiredOrgCount;
    private LocalDateTime confirmDeadline;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
