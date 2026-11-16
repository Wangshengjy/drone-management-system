package com.drone.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mission_log")
public class MissionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long missionId;
    private String action;
    private Long operatorId;
    private String commentText;
    private String attachmentUrl;
    private String detailJson;
    private LocalDateTime createdAt;
}
