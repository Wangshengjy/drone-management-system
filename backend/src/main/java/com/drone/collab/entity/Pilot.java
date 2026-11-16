package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pilot")
public class Pilot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String fullName;
    private String idCard;
    private String phone;
    private String email;
    private String recordNo;
    private String region;
    /** DRAFT, PENDING_REVIEW, APPROVED, REJECTED, FROZEN */
    private String status;
    private String autoResult;
    private Integer autoScore;
    private String autoReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
