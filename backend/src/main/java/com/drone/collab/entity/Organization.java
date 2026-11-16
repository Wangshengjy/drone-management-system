package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("organization")
public class Organization {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    /** ENTERPRISE, VOLUNTEER, OTHER */
    private String orgType;
    private String creditCode;
    private String contactName;
    private String phone;
    private String address;
    private String status;
    private String autoResult;
    private Integer autoScore;
    private String autoReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
