package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mission_assignment")
public class MissionAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long missionId;
    private String assigneeType;
    private Long assigneeId;
    private Double matchScore;
    private String matchDetail;
    private String status;
    private LocalDateTime respondedAt;
    private LocalDateTime completedAt;
    private Integer rankOrder;
    private LocalDateTime createdAt;
}
