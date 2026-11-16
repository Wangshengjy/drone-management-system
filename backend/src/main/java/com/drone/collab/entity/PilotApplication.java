package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 飞手报名/提交审核记录 */
@Data
@TableName("pilot_application")
public class PilotApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pilotId;
    private String applyType;
    private String status;
    private String autoResult;
    private Integer autoScore;
    private String autoReason;
    private LocalDateTime createdAt;
}
