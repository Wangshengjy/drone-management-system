package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;

    @TableField("real_name")
    private String realName;

    private String phone;

    /** ADMIN, ORG, PILOT, GUEST */
    private String role;

    @TableField("pilot_id")
    private Long pilotId;

    @TableField("org_id")
    private Long orgId;

    @TableField("status")
    private Integer enabled;

    @TableField("create_time")
    private LocalDateTime createdAt;

    @TableField("update_time")
    private LocalDateTime updatedAt;
}
