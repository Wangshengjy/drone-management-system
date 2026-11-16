package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pilot_status")
public class PilotStatus {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pilotId;
    private String availability;
    private Double currentLatitude;
    private Double currentLongitude;
    private String droneType;
    private Integer missionCount;
    private Long activeMissionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
