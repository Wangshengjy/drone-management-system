package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pilot_org")
public class PilotOrg {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pilotId;
    private Long orgId;
    private String status;
    private String initiator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String pilotName;
    @TableField(exist = false)
    private String pilotPhone;
    @TableField(exist = false)
    private String pilotStatus;
    @TableField(exist = false)
    private String orgName;
    @TableField(exist = false)
    private String orgType;
    @TableField(exist = false)
    private String orgStatus;
    @TableField(exist = false)
    private String orgContactPerson;
}
